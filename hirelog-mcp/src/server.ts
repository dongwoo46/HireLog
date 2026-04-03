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

async function fetchText(url: string): Promise<string> {
  const headers: Record<string, string> = {
    Accept: "application/json,text/plain,*/*"
  };

  if (API_BEARER_TOKEN) {
    headers.Authorization = `Bearer ${API_BEARER_TOKEN}`;
  }
  if (API_COOKIE) {
    headers.Cookie = API_COOKIE;
  }

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
  if (API_BEARER_TOKEN) {
    headers.Authorization = `Bearer ${API_BEARER_TOKEN}`;
  }
  if (API_COOKIE) {
    headers.Cookie = API_COOKIE;
  }

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

export function createHirelogServer(options?: { publicReadOnly?: boolean }) {
  const readOnly = options?.publicReadOnly ?? MCP_PUBLIC_READONLY;
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
        const body = await fetchText(url);
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
        const body = await fetchText(url);
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
        const body = await fetchJson(endpoint, "POST", {
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
            "If auth is required, set:",
            "HIRELOG_API_BEARER_TOKEN=<access_token>"
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
        const body = await fetchJson(endpoint, "POST", {
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
            "If auth is required, set:",
            "HIRELOG_API_BEARER_TOKEN=<access_token>"
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
        const body = await fetchJson(endpoint, "GET");
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
        const body = await fetchJson(endpoint, "GET");
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
        const body = await fetchJson(endpoint, "GET");
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
