// Public API of core/api — re-exports for consumers
export type { IApiAdapter, RequestOptions, ApiResponse, ApiError } from "./types"
export { HttpApiAdapter, createHttpApi } from "./http-client"
export { MockApiAdapter } from "./mock-adapter"
