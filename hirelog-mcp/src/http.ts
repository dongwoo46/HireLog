import { createMcpExpressApp } from "@modelcontextprotocol/sdk/server/express.js";
import { StreamableHTTPServerTransport } from "@modelcontextprotocol/sdk/server/streamableHttp.js";
import { createHirelogServer } from "./server.js";

const PORT = Number(process.env.PORT ?? 8787);
const HOST = process.env.HOST ?? "0.0.0.0";
const MCP_PATH = process.env.MCP_PATH ?? "/mcp";
const MCP_AUTH_TOKEN = process.env.MCP_AUTH_TOKEN;

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

const app = createMcpExpressApp({ host: HOST });

app.post(MCP_PATH, async (req: any, res: any) => {
  if (!isAuthorized(req)) {
    deny(res);
    return;
  }

  const server = createHirelogServer();
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
