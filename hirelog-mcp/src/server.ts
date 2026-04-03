import { Server } from "@modelcontextprotocol/sdk/server/index.js";
import {
  CallToolRequestSchema,
  ListToolsRequestSchema
} from "@modelcontextprotocol/sdk/types.js";

const RAW_API_BASE_URL =
  process.env.HIRELOG_API_BASE_URL ??
  process.env.VITE_API_BASE_URL ??
  "https://hirelog.kro.kr";
const API_BEARER_TOKEN = process.env.HIRELOG_API_BEARER_TOKEN;
const API_COOKIE = process.env.HIRELOG_API_COOKIE;
const MCP_PUBLIC_READONLY = (process.env.MCP_PUBLIC_READONLY ?? "false").toLowerCase() === "true";
const WRITE_OR_PRIVATE_TOOLS = new Set(["jd_register", "jd_register_text", "my_applied_jd", "my_saved_jd"]);
const DEFAULT_AUTH_CONTEXT_KEY = "global";

type SessionAuth = {
  accessToken: string;
  refreshToken?: string;
  email?: string;
  updatedAt: string;
};

const sessionAuthByContext = new Map<string, SessionAuth>();

export function isWriteOrPrivateTool(toolName: string): boolean {
  return WRITE_OR_PRIVATE_TOOLS.has(toolName);
}

function buildApiBaseUrl(raw: string): string {
  const trimmed = raw.trim().replace(/\/+$/, "");
  if (!trimmed) {
    return "";
  }
  return trimmed.endsWith("/api") ? trimmed : `${trimmed}/api`;
}

const API_BASE_URL = buildApiBaseUrl(RAW_API_BASE_URL);

function apiUrl(pathname: string, query?: URLSearchParams): string {
  const normalizedPath = pathname.startsWith("/") ? pathname : `/${pathname}`;
  const qs = query && query.toString() ? `?${query.toString()}` : "";
  return `${API_BASE_URL}${normalizedPath}${qs}`;
}

function text(content: string) {
  return {
    content: [
      {
        type: "text" as const,
        text: content
      }
    ]
  };
}

function getSessionAuth(contextKey: string): SessionAuth | undefined {
  return sessionAuthByContext.get(contextKey);
}

function setSessionAuth(contextKey: string, auth: SessionAuth) {
  sessionAuthByContext.set(contextKey, auth);
}

function clearSessionAuth(contextKey: string) {
  sessionAuthByContext.delete(contextKey);
}

function buildAuthHeaders(contextKey: string): Record<string, string> {
  const headers: Record<string, string> = {};
  const sessionAuth = getSessionAuth(contextKey);
  const cookieParts: string[] = [];

  if (API_COOKIE) {
    cookieParts.push(API_COOKIE);
  }
  if (sessionAuth?.accessToken) {
    headers.Authorization = `Bearer ${sessionAuth.accessToken}`;
    cookieParts.push(`access_token=${sessionAuth.accessToken}`);
  } else if (API_BEARER_TOKEN) {
    headers.Authorization = `Bearer ${API_BEARER_TOKEN}`;
  }
  if (sessionAuth?.refreshToken) {
    cookieParts.push(`refresh_token=${sessionAuth.refreshToken}`);
  }
  if (cookieParts.length > 0) {
    headers.Cookie = cookieParts.join("; ");
  }

  return headers;
}

async function fetchText(url: string, contextKey: string): Promise<string> {
  const headers: Record<string, string> = {
    Accept: "application/json,text/plain,*/*"
  };
  Object.assign(headers, buildAuthHeaders(contextKey));

  const res = await fetch(url, {
    method: "GET",
    headers
  });
  const body = await res.text();
  if (!res.ok) {
    throw new Error(`HTTP ${res.status} from ${url}\n${body}`);
  }
  return body;
}

async function fetchJson(
  url: string,
  contextKey: string,
  method: "GET" | "POST" = "GET",
  payload?: Record<string, unknown>
): Promise<string> {
  const headers: Record<string, string> = {
    Accept: "application/json,text/plain,*/*"
  };

  headers.Accept = "application/json,text/plain,*/*";
  if (payload) {
    headers["Content-Type"] = "application/json";
  }
  Object.assign(headers, buildAuthHeaders(contextKey));

  const res = await fetch(url, {
    method,
    headers,
    body: payload ? JSON.stringify(payload) : undefined
  });
  const body = await res.text();
  if (!res.ok) {
    throw new Error(`${method} ${url} failed with HTTP ${res.status}\n${body}`);
  }
  return body;
}

export function createHirelogServer(options?: { publicReadOnly?: boolean; authContextKey?: string }) {
  const readOnly = options?.publicReadOnly ?? MCP_PUBLIC_READONLY;
  const authContextKey = options?.authContextKey?.trim() || DEFAULT_AUTH_CONTEXT_KEY;
  const server = new Server(
    {
      name: "hirelog-mcp",
      version: "0.2.0"
    },
    {
      capabilities: {
        tools: {}
      }
    }
  );

  server.setRequestHandler(ListToolsRequestSchema, async () => {
    const tools: Array<Record<string, unknown>> = [
      {
        name: "ping",
        description: "Check whether hirelog-mcp is alive.",
        inputSchema: {
          type: "object",
          properties: {}
        }
      },
      {
        name: "hirelog_health",
        description: "Call HireLog API health endpoint (/actuator/health).",
        inputSchema: {
          type: "object",
          properties: {}
        }
      },
      {
        name: "search_jd",
        description:
          "Search JD list. Maps to GET /api/job-summary/search (keyword, size).",
        inputSchema: {
          type: "object",
          properties: {
            query: {
              type: "string",
              description: "Search keyword."
            },
            limit: {
              type: "number",
              description: "Maximum number of items to request.",
              minimum: 1,
              maximum: 50,
              default: 10
            }
          },
          required: ["query"]
        }
      },
      {
        name: "jd_get_detail",
        description: "Get JD detail by jobSummaryId. Maps to GET /api/job-summary/{id}.",
        inputSchema: {
          type: "object",
          properties: {
            jobSummaryId: { type: "number" }
          },
          required: ["jobSummaryId"]
        }
      },
      {
        name: "jd_list",
        description:
          "List JD summaries. Maps to GET /api/job-summary/search with optional filters.",
        inputSchema: {
          type: "object",
          properties: {
            keyword: { type: "string" },
            careerType: { type: "string" },
            brandId: { type: "number" },
            companyId: { type: "number" },
            positionId: { type: "number" },
            brandPositionId: { type: "number" },
            positionCategoryId: { type: "number" },
            brandName: { type: "string" },
            positionName: { type: "string" },
            brandPositionName: { type: "string" },
            positionCategoryName: { type: "string" },
            techStacks: {
              type: "array",
              items: { type: "string" }
            },
            cursor: { type: "string" },
            size: { type: "number", minimum: 1, maximum: 100 },
            sortBy: { type: "string" }
          }
        }
      },
      {
        name: "auth_login",
        description:
          "Login with email/password via POST /api/auth/login and store tokens for this MCP session.",
        inputSchema: {
          type: "object",
          properties: {
            email: { type: "string" },
            password: { type: "string" }
          },
          required: ["email", "password"]
        }
      },
      {
        name: "auth_status",
        description: "Show whether this MCP session currently has stored auth tokens.",
        inputSchema: {
          type: "object",
          properties: {}
        }
      },
      {
        name: "auth_logout",
        description: "Logout this MCP session and clear stored tokens.",
        inputSchema: {
          type: "object",
          properties: {}
        }
      },
      {
        name: "auth_set_tokens",
        description:
          "Set access/refresh token directly for this MCP session (useful for social login tokens).",
        inputSchema: {
          type: "object",
          properties: {
            accessToken: { type: "string" },
            refreshToken: { type: "string" },
            email: { type: "string" }
          },
          required: ["accessToken"]
        }
      },
      {
        name: "auth_oauth_url",
        description: "Get OAuth login URL for social providers (google, kakao).",
        inputSchema: {
          type: "object",
          properties: {
            provider: { type: "string", enum: ["google", "kakao"] }
          },
          required: ["provider"]
        }
      }
    ];

    if (!readOnly) {
      tools.push(
        {
          name: "jd_register",
          description:
            "Register JD by URL. Maps to POST /api/job-summary/url (requires auth token).",
          inputSchema: {
            type: "object",
            properties: {
              brandName: { type: "string" },
              brandPositionName: { type: "string" },
              url: { type: "string" }
            },
            required: ["brandName", "brandPositionName", "url"]
          }
        },
        {
          name: "jd_register_text",
          description:
            "Register JD by raw text. Maps to POST /api/job-summary/text (requires auth token).",
          inputSchema: {
            type: "object",
            properties: {
              brandName: { type: "string" },
              brandPositionName: { type: "string" },
              jdText: { type: "string" }
            },
            required: ["brandName", "brandPositionName", "jdText"]
          }
        },
        {
          name: "my_applied_jd",
          description:
            "List my applied JD items. Maps to GET /api/member-job-summary?saveType=APPLY.",
          inputSchema: {
            type: "object",
            properties: {
              page: { type: "number", minimum: 0, default: 0 },
              size: { type: "number", minimum: 1, maximum: 100, default: 20 }
            }
          }
        },
        {
          name: "my_saved_jd",
          description:
            "List my saved JD items. Maps to GET /api/member-job-summary?saveType=SAVED.",
          inputSchema: {
            type: "object",
            properties: {
              page: { type: "number", minimum: 0, default: 0 },
              size: { type: "number", minimum: 1, maximum: 100, default: 20 }
            }
          }
        }
      );
    }

    return {
      tools
    };
  });

  server.setRequestHandler(CallToolRequestSchema, async (request) => {
    const { name, arguments: rawArgs } = request.params;
    const args = (rawArgs ?? {}) as Record<string, unknown>;

    if (name === "ping") {
      return text("pong");
    }

    if (name === "hirelog_health") {
      const url = RAW_API_BASE_URL.endsWith("/api")
        ? `${RAW_API_BASE_URL.replace(/\/+$/, "").replace(/\/api$/, "")}/actuator/health`
        : `${RAW_API_BASE_URL.replace(/\/+$/, "")}/actuator/health`;
      try {
        const body = await fetchText(url, authContextKey);
        return text(`Health OK\n${body}`);
      } catch (error) {
        const message = error instanceof Error ? error.message : String(error);
        return text(`Health check failed\n${message}`);
      }
    }

    if (name === "search_jd") {
      const query = String(args.query ?? "").trim();
      const limit = Number(args.limit ?? 10);

      if (!query) {
        return text("`query` is required.");
      }

      const search = new URLSearchParams();
      search.set("keyword", query);
      search.set("size", String(Math.min(Math.max(1, Number.isFinite(limit) ? limit : 10), 100)));

      const url = apiUrl("/job-summary/search", search);

      try {
        const body = await fetchText(url, authContextKey);
        return text(body);
      } catch (error) {
        const message = error instanceof Error ? error.message : String(error);
        return text(
          [
            "JD search failed.",
            message,
            "",
            "Set external API base URL if needed:",
            "HIRELOG_API_BASE_URL=https://your-api-domain.com"
          ].join("\n")
        );
      }
    }

    if (name === "auth_login") {
      const email = String(args.email ?? "").trim();
      const password = String(args.password ?? "");
      if (!email || !password) {
        return text("`email`, `password` are required.");
      }

      const endpoint = apiUrl("/auth/login");
      try {
        const body = await fetchJson(endpoint, authContextKey, "POST", { email, password });
        const parsed = JSON.parse(body) as { accessToken?: unknown; refreshToken?: unknown };
        const accessToken = typeof parsed.accessToken === "string" ? parsed.accessToken.trim() : "";
        const refreshToken = typeof parsed.refreshToken === "string" ? parsed.refreshToken.trim() : "";

        if (!accessToken) {
          return text(`Login succeeded but no accessToken was returned.\n${body}`);
        }

        setSessionAuth(authContextKey, {
          accessToken,
          refreshToken: refreshToken || undefined,
          email,
          updatedAt: new Date().toISOString()
        });

        return text(
          JSON.stringify(
            {
              ok: true,
              message: "Logged in. This MCP session will now use the token for write/private APIs.",
              accessToken,
              refreshToken: refreshToken || null
            },
            null,
            2
          )
        );
      } catch (error) {
        const message = error instanceof Error ? error.message : String(error);
        return text(`Login failed.\n${message}`);
      }
    }

    if (name === "auth_status") {
      const sessionAuth = getSessionAuth(authContextKey);
      return text(
        JSON.stringify(
          {
            authenticated: Boolean(sessionAuth?.accessToken),
            email: sessionAuth?.email ?? null,
            hasRefreshToken: Boolean(sessionAuth?.refreshToken),
            updatedAt: sessionAuth?.updatedAt ?? null
          },
          null,
          2
        )
      );
    }

    if (name === "auth_logout") {
      const endpoint = apiUrl("/auth/logout");
      try {
        await fetchJson(endpoint, authContextKey, "POST", {});
      } catch {
        // Ignore remote logout errors and always clear local session state.
      }
      clearSessionAuth(authContextKey);
      return text("Logged out and cleared MCP session tokens.");
    }

    if (name === "auth_set_tokens") {
      const accessToken = String(args.accessToken ?? "").trim();
      const refreshToken = String(args.refreshToken ?? "").trim();
      const email = String(args.email ?? "").trim();

      if (!accessToken) {
        return text("`accessToken` is required.");
      }

      setSessionAuth(authContextKey, {
        accessToken,
        refreshToken: refreshToken || undefined,
        email: email || undefined,
        updatedAt: new Date().toISOString()
      });

      return text(
        JSON.stringify(
          {
            ok: true,
            message: "Tokens saved. This MCP session will use them for write/private APIs.",
            hasRefreshToken: Boolean(refreshToken)
          },
          null,
          2
        )
      );
    }

    if (name === "auth_oauth_url") {
      const provider = String(args.provider ?? "").trim().toLowerCase();
      if (provider !== "google" && provider !== "kakao") {
        return text("`provider` must be one of: google, kakao");
      }
      const base = RAW_API_BASE_URL.replace(/\/+$/, "").replace(/\/api$/, "");
      const url = `${base}/oauth2/authorization/${provider}`;
      return text(
        JSON.stringify(
          {
            provider,
            oauthUrl: url,
            note: "Open this URL in a browser to complete social login."
          },
          null,
          2
        )
      );
    }

    if (readOnly && isWriteOrPrivateTool(name)) {
      return text(
        [
          "This tool is disabled in public read-only mode.",
          "Set MCP_PUBLIC_READONLY=false to enable write/private tools."
        ].join("\n")
      );
    }

    if (name === "jd_register") {
      const brandName = String(args.brandName ?? "").trim();
      const brandPositionName = String(args.brandPositionName ?? "").trim();
      const url = String(args.url ?? "").trim();

      if (!brandName || !brandPositionName || !url) {
        return text("`brandName`, `brandPositionName`, `url` are required.");
      }

      const endpoint = apiUrl("/job-summary/url");
      try {
        const body = await fetchJson(endpoint, authContextKey, "POST", {
          brandName,
          brandPositionName,
          url
        });
        return text(body);
      } catch (error) {
        const message = error instanceof Error ? error.message : String(error);
        return text(
          [
            "JD register failed.",
            message,
            "",
            "If auth is required, run `auth_login` first or set:",
            "HIRELOG_API_BEARER_TOKEN=<access_token> (legacy fallback)"
          ].join("\n")
        );
      }
    }

    if (name === "jd_register_text") {
      const brandName = String(args.brandName ?? "").trim();
      const brandPositionName = String(args.brandPositionName ?? "").trim();
      const jdText = String(args.jdText ?? "").trim();

      if (!brandName || !brandPositionName || !jdText) {
        return text("`brandName`, `brandPositionName`, `jdText` are required.");
      }

      const endpoint = apiUrl("/job-summary/text");
      try {
        const body = await fetchJson(endpoint, authContextKey, "POST", {
          brandName,
          brandPositionName,
          jdText
        });
        return text(body);
      } catch (error) {
        const message = error instanceof Error ? error.message : String(error);
        return text(
          [
            "JD text register failed.",
            message,
            "",
            "If auth is required, run `auth_login` first or set:",
            "HIRELOG_API_BEARER_TOKEN=<access_token> (legacy fallback)"
          ].join("\n")
        );
      }
    }

    if (name === "jd_get_detail") {
      const jobSummaryId = Number(args.jobSummaryId);
      if (!Number.isFinite(jobSummaryId)) {
        return text("`jobSummaryId` must be a number.");
      }

      const endpoint = apiUrl(`/job-summary/${jobSummaryId}`);
      try {
        const body = await fetchJson(endpoint, authContextKey, "GET");
        return text(body);
      } catch (error) {
        const message = error instanceof Error ? error.message : String(error);
        return text(`JD detail failed.\n${message}`);
      }
    }

    if (name === "jd_list") {
      const query = new URLSearchParams();
      const keys = [
        "keyword",
        "careerType",
        "brandId",
        "companyId",
        "positionId",
        "brandPositionId",
        "positionCategoryId",
        "brandName",
        "positionName",
        "brandPositionName",
        "positionCategoryName",
        "cursor",
        "size",
        "sortBy"
      ] as const;

      for (const key of keys) {
        const value = args[key];
        if (value !== undefined && value !== null && String(value).trim() !== "") {
          query.set(key, String(value));
        }
      }

      if (Array.isArray(args.techStacks)) {
        for (const stack of args.techStacks) {
          if (typeof stack === "string" && stack.trim()) {
            query.append("techStacks", stack.trim());
          }
        }
      }

      const endpoint = apiUrl("/job-summary/search", query);
      try {
        const body = await fetchJson(endpoint, authContextKey, "GET");
        return text(body);
      } catch (error) {
        const message = error instanceof Error ? error.message : String(error);
        return text(`JD list failed.\n${message}`);
      }
    }

    if (name === "my_applied_jd" || name === "my_saved_jd") {
      const page = Number.isFinite(Number(args.page)) ? Number(args.page) : 0;
      const size = Number.isFinite(Number(args.size)) ? Number(args.size) : 20;
      const saveType = name === "my_applied_jd" ? "APPLY" : "SAVED";

      const query = new URLSearchParams();
      query.set("saveType", saveType);
      query.set("page", String(Math.max(0, page)));
      query.set("size", String(Math.min(Math.max(1, size), 100)));

      const endpoint = apiUrl("/member-job-summary", query);
      try {
        const body = await fetchJson(endpoint, authContextKey, "GET");
        return text(body);
      } catch (error) {
        const message = error instanceof Error ? error.message : String(error);
        return text(
          [
            `${saveType} list failed.`,
            message,
            "",
            "This endpoint usually requires auth. Set:",
            "HIRELOG_API_BEARER_TOKEN=<access_token>"
          ].join("\n")
        );
      }
    }

    return text(`Unknown tool: ${name}`);
  });

  return server;
}
