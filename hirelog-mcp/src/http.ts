import { createMcpExpressApp } from "@modelcontextprotocol/sdk/server/express.js";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";
import { createHirelogServer, isWriteOrPrivateTool } from "./server.js";

const PORT = Number(process.env.PORT ?? 8787);
const HOST = process.env.HOST ?? "0.0.0.0";
const MCP_PATH = process.env.MCP_PATH ?? "/mcp";
const MCP_AUTH_TOKEN = process.env.MCP_AUTH_TOKEN;
const READ_RATE_LIMIT_PER_MIN = Number(process.env.MCP_READ_RATE_LIMIT_PER_MIN ?? 120);
const WRITE_RATE_LIMIT_PER_MIN = Number(process.env.MCP_WRITE_RATE_LIMIT_PER_MIN ?? 20);
const RATE_LIMIT_WINDOW_MS = 60_000;

type Bucket = "read" | "write";
type Counter = { count: number; windowStart: number };

const rateCounters = new Map<string, Counter>();

function deny(res: any) {
  res.status(401).json({
    jsonrpc: "2.0",
    error: {
      code: -32001,
      message: "Unauthorized"
    },
    id: null
  });
}

function tooManyRequests(res: any, bucket: Bucket) {
  res.status(429).json({
    jsonrpc: "2.0",
    error: {
      code: -32029,
      message: `Rate limit exceeded for ${bucket} requests`
    },
    id: null
  });
}

function getJsonRpcMethod(body: any): string | undefined {
  if (!body || typeof body !== "object" || Array.isArray(body)) {
    return undefined;
  }
  const method = body.method;
  return typeof method === "string" ? method : undefined;
}

function getCalledToolName(body: any): string | undefined {
  if (!body || typeof body !== "object" || Array.isArray(body)) {
    return undefined;
  }
  const params = body.params;
  if (!params || typeof params !== "object" || Array.isArray(params)) {
    return undefined;
  }
  const name = (params as Record<string, unknown>).name;
  return typeof name === "string" ? name : undefined;
}

function isAuthorized(req: any): boolean {
  if (!MCP_AUTH_TOKEN) {
    return true;
  }
  const auth = req.header("authorization");
  if (!auth) {
    return false;
  }
  const expected = `Bearer ${MCP_AUTH_TOKEN}`;
  return auth.trim() === expected;
}

function getClientKey(req: any): string {
  const auth = req.header("authorization");
  if (typeof auth === "string") {
    const match = auth.match(/^Bearer\s+(.+)$/i);
    if (match && match[1].trim()) {
      return `token:${match[1].trim()}`;
    }
  }

  const xff = req.header("x-forwarded-for");
  if (typeof xff === "string" && xff.trim()) {
    const firstIp = xff.split(",")[0]?.trim();
    if (firstIp) {
      return `ip:${firstIp}`;
    }
  }

  const remote = req.ip ?? req.socket?.remoteAddress ?? "unknown";
  return `ip:${String(remote)}`;
}

function checkRateLimit(req: any, bucket: Bucket): boolean {
  const limit = bucket === "write" ? WRITE_RATE_LIMIT_PER_MIN : READ_RATE_LIMIT_PER_MIN;
  if (!Number.isFinite(limit) || limit <= 0) {
    return true;
  }

  const now = Date.now();
  const key = `${bucket}:${getClientKey(req)}`;
  const current = rateCounters.get(key);

  if (!current || now - current.windowStart >= RATE_LIMIT_WINDOW_MS) {
    rateCounters.set(key, { count: 1, windowStart: now });
    return true;
  }

  if (current.count >= limit) {
    return false;
  }

  current.count += 1;
  return true;
}

const app = createMcpExpressApp({ host: HOST });

app.post(MCP_PATH, async (req: any, res: any) => {
  const authorized = isAuthorized(req);
  const tokenConfigured = Boolean(MCP_AUTH_TOKEN);
  const jsonRpcMethod = getJsonRpcMethod(req.body);
  const calledToolName = getCalledToolName(req.body);
  const isWriteToolCall =
    jsonRpcMethod === "tools/call" &&
    typeof calledToolName === "string" &&
    isWriteOrPrivateTool(calledToolName);
  const bucket: Bucket = isWriteToolCall ? "write" : "read";

  // Mixed-mode policy:
  // - If token is configured and caller is unauthorized:
  //   - read-only methods are allowed
  //   - write/private tool calls are blocked
  if (tokenConfigured && !authorized && isWriteToolCall) {
    deny(res);
    return;
  }

  if (!checkRateLimit(req, bucket)) {
    tooManyRequests(res, bucket);
    return;
  }

  const forceReadOnly = tokenConfigured && !authorized;
  const server = createHirelogServer({
    publicReadOnly: forceReadOnly ? true : undefined
  });
  const transport = new StreamableHTTPServerTransport({
    sessionIdGenerator: undefined
  });

  try {
    await server.connect(transport);
    await transport.handleRequest(req, res, req.body);
  } catch (error) {
    console.error("Error handling MCP request:", error);
    if (!res.headersSent) {
      res.status(500).json({
        jsonrpc: "2.0",
        error: {
          code: -32603,
          message: "Internal server error"
        },
        id: null
      });
    }
  } finally {
    res.on("close", () => {
      transport.close();
      server.close();
    });
  }
});

app.get(MCP_PATH, async (_req: any, res: any) => {
  res.status(405).json({
    jsonrpc: "2.0",
    error: {
      code: -32000,
      message: "Method not allowed."
    },
    id: null
  });
});

app.delete(MCP_PATH, async (_req: any, res: any) => {
  res.status(405).json({
    jsonrpc: "2.0",
    error: {
      code: -32000,
      message: "Method not allowed."
    },
    id: null
  });
});

app.get("/", (_req: any, res: any) => {
  res.status(200).send("hirelog-mcp http server is running");
});

app.listen(PORT, HOST, (error?: Error) => {
  if (error) {
    console.error("Failed to start MCP HTTP server:", error);
    process.exit(1);
  }
  console.log(`hirelog-mcp http listening on http://${HOST}:${PORT}${MCP_PATH}`);
});
