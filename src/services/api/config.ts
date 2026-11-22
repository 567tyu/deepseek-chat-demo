/**
 * 运行时 API 配置：直接读写 localStorage，避免与 settings / speech 形成循环依赖。
 * 端点、Key、当前模型、手动添加与拉取到的模型列表都保存在本机。
 */
export const DEFAULT_API_ORIGIN = 'https://api.deepseek.com'
export const DEFAULT_MODEL = 'deepseek-flash'

export const STORAGE_KEYS = {
  apiEndpoint: 'chat-demo:api-endpoint',
  apiKey: 'chat-demo:api-key',
  model: 'chat-demo:model',
  customModels: 'chat-demo:custom-models',
  pulledModels: 'chat-demo:pulled-models',
} as const

const readRaw = (key: string): string | null => {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

const writeRaw = (key: string, value: string | null) => {
  try {
    if (value === null) {
      localStorage.removeItem(key)
    } else {
      localStorage.setItem(key, value)
    }
  } catch {
    // 存储不可用时只在内存中生效
  }
}

/** 当前 API 端点（基础地址），未填写时回退到默认地址 */
export const getApiOrigin = (): string =>
  readRaw(STORAGE_KEYS.apiEndpoint)?.trim() || DEFAULT_API_ORIGIN

/** 当前选中的模型名 */
export const getSelectedModel = (): string =>
  readRaw(STORAGE_KEYS.model)?.trim() || DEFAULT_MODEL

/** 读取字符串列表（手动添加 / 拉取到的模型） */
export const readModelList = (key: string): string[] => {
  try {
    const parsed: unknown = JSON.parse(readRaw(key) ?? '[]')
    return Array.isArray(parsed)
      ? parsed.filter((item): item is string => typeof item === 'string' && item.trim() !== '')
      : []
  } catch {
    return []
  }
}

/** 写入字符串列表 */
export const writeModelList = (key: string, list: string[]) =>
  writeRaw(key, JSON.stringify(list))
