import { create } from 'zustand'

/** 模型选择底部弹窗的开合状态；可由设置面板或输入框中间的模型框打开 */
export const useModelPicker = create(() => ({ open: false }))

export const openModelPicker = () => useModelPicker.setState({ open: true })

export const closeModelPicker = () => useModelPicker.setState({ open: false })
