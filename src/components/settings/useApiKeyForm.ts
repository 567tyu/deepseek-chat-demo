import { useState } from 'react'
import { setApiKey } from '../../services/settings.js'

/**
 * 输入 API Key 后直接保存到本机；不再调用服务端 /models 做预校验。
 * 保留 verifying / error 字段以兼容设置面板的渲染逻辑。
 */
export const useApiKeyForm = (onSaved?: () => void) => {
  const [value, setValue] = useState('')

  const submit = () => {
    const apiKey = value.trim()
    if (!apiKey) {
      return
    }
    setApiKey(apiKey)
    setValue('')
    onSaved?.()
  }

  return { value, setValue, verifying: false, error: null, submit }
}
