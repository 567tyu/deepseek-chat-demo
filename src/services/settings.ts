import { create } from 'zustand'
import type { TtsVoice } from './api/speech.js'
import { normalizeLocation, type UserLocation } from './location.js'
import {
  STORAGE_KEYS,
  getApiOrigin,
  getSelectedModel,
  readModelList,
  writeModelList,
} from './api/config.js'

export type Locale = 'zh-CN' | 'en-US'

/** 主题偏好；默认跟随系统 */
export type ThemePreference = 'light' | 'dark' | 'system'

/** 本机设置，存 localStorage：API 端点、API Key、模型、模型列表、语言、主题、位置、朗读音色、思考与搜索开关、侧边栏收起状态 */
type Settings = {
  apiKey: string | null
  apiEndpoint: string
  model: string
  customModels: string[]
  pulledModels: string[]
  locale: Locale
  themePreference: ThemePreference
  location: UserLocation | null
  ttsVoice: TtsVoice
  thinking: boolean
  search: boolean
  siderCollapsed: boolean
}

const KEYS = {
  locale: 'chat-demo:locale',
  location: 'chat-demo:location',
  themePreference: 'chat-demo:theme-preference',
  ttsVoice: 'chat-demo:tts-voice',
  thinking: 'chat-demo:thinking',
  search: 'chat-demo:search',
  siderCollapsed: 'chat-demo:sider-collapsed',
} as const

const read = (key: string) => {
  try {
    return localStorage.getItem(key)
  } catch {
    return null
  }
}

const write = (key: string, value: string | null) => {
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

/** 位置选择模式跟随浏览器语言，与应用内选择的界面语言无关。 */
export const isChineseBrowser = (): boolean =>
  (typeof navigator === 'undefined' ? 'zh' : navigator.language).toLowerCase().startsWith('zh')

const initialLocale = (): Locale => {
  const saved = read(KEYS.locale)
  if (saved === 'zh-CN' || saved === 'en-US') {
    return saved
  }
  return isChineseBrowser() ? 'zh-CN' : 'en-US'
}

const initialLocation = (): UserLocation | null => {
  try {
    const location = normalizeLocation(JSON.parse(read(KEYS.location) ?? 'null'))
    if (!location) {
      return null
    }
    return isChineseBrowser()
      ? location.region === 'CN' && location.province
        ? location
        : null
      : { region: location.region }
  } catch {
    return null
  }
}

/** 未保存过时跟随系统 */
const initialThemePreference = (): ThemePreference => {
  const saved = read(KEYS.themePreference)
  return saved === 'light' || saved === 'dark' ? saved : 'system'
}

/** 未保存过时使用服务端默认音色 stella（海星） */
const initialTtsVoice = (): TtsVoice => (read(KEYS.ttsVoice) === 'tide' ? 'tide' : 'stella')

export const useSettings = create<Settings>(() => ({
  apiKey: read(STORAGE_KEYS.apiKey),
  apiEndpoint: getApiOrigin(),
  model: getSelectedModel(),
  customModels: readModelList(STORAGE_KEYS.customModels),
  pulledModels: readModelList(STORAGE_KEYS.pulledModels),
  locale: initialLocale(),
  location: initialLocation(),
  themePreference: initialThemePreference(),
  ttsVoice: initialTtsVoice(),
  thinking: read(KEYS.thinking) === 'true',
  search: read(KEYS.search) === 'true',
  siderCollapsed: read(KEYS.siderCollapsed) === 'true',
}))

export const setLocale = (locale: Locale) => {
  write(KEYS.locale, locale)
  useSettings.setState({ locale })
}

export const setLocation = (value: UserLocation | null) => {
  const location = normalizeLocation(value)
  write(KEYS.location, location ? JSON.stringify(location) : null)
  useSettings.setState({ location })
}

export const setThemePreference = (themePreference: ThemePreference) => {
  write(KEYS.themePreference, themePreference)
  useSettings.setState({ themePreference })
}

export const setTtsVoice = (ttsVoice: TtsVoice) => {
  write(KEYS.ttsVoice, ttsVoice)
  useSettings.setState({ ttsVoice })
}

export const setApiKey = (apiKey: string | null) => {
  write(STORAGE_KEYS.apiKey, apiKey)
  useSettings.setState({ apiKey })
}

export const setApiEndpoint = (apiEndpoint: string) => {
  const value = apiEndpoint.trim()
  write(STORAGE_KEYS.apiEndpoint, value || null)
  // 允许留空：留空时请求层再回退默认端点，避免输入框被默认值回填卡住
  useSettings.setState({ apiEndpoint: value })
}

export const setModel = (model: string) => {
  const value = model.trim()
  write(STORAGE_KEYS.model, value || null)
  // 允许留空：留空时请求层再回退默认模型，避免输入框被默认值回填卡住
  useSettings.setState({ model: value })
}

/** 合并去重：手动添加的模型排在前面，其次是拉取到的模型 */
export const allModels = (state: Pick<Settings, 'customModels' | 'pulledModels'>): string[] =>
  [...new Set([...state.customModels, ...state.pulledModels])]

export const setPulledModels = (pulledModels: string[]) => {
  writeModelList(STORAGE_KEYS.pulledModels, pulledModels)
  useSettings.setState({ pulledModels })
}

/** 添加一个手动模型；已存在时不重复写入。返回是否新添加。 */
export const addCustomModel = (model: string): boolean => {
  const value = model.trim()
  if (!value) {
    return false
  }
  const current = useSettings.getState().customModels
  if (current.includes(value)) {
    return false
  }
  const next = [...current, value]
  writeModelList(STORAGE_KEYS.customModels, next)
  useSettings.setState({ customModels: next })
  return true
}

export const removeCustomModel = (model: string) => {
  const next = useSettings
    .getState()
    .customModels.filter((item) => item !== model)
  writeModelList(STORAGE_KEYS.customModels, next)
  useSettings.setState({ customModels: next })
}

export const setThinking = (thinking: boolean) => {
  write(KEYS.thinking, String(thinking))
  useSettings.setState({ thinking })
}

export const setSearch = (search: boolean) => {
  write(KEYS.search, String(search))
  useSettings.setState({ search })
}

export const setSiderCollapsed = (siderCollapsed: boolean) => {
  write(KEYS.siderCollapsed, String(siderCollapsed))
  useSettings.setState({ siderCollapsed })
}
