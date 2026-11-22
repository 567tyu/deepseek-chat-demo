import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import clsx from 'clsx'
import { fetchModelList } from '../../services/api/client.js'
import {
  addCustomModel,
  allModels,
  setModel,
  setPulledModels,
  useSettings,
} from '../../services/settings.js'
import { useTranslate } from '../../i18n/index.js'
import { IconCheckOutline16, IconCloseOutline16, IconRefreshOutline16 } from '../base/icons.js'
import { showToast } from '../base/toastState.js'
import { closeModelPicker, useModelPicker } from './modelPickerState.js'
import styles from './ModelPicker.module.css'

/**
 * 模型选择底部弹窗：
 * - 「拉取模型」从当前端点 GET /models 拉取可用模型，成功后填入列表；
 * - 列表合并手动添加与拉取到的模型，点击即选中；
 * - 输入模型名可手动添加到列表后再选择。
 */
export const ModelPicker = () => {
  const t = useTranslate()
  const open = useModelPicker((state) => state.open)
  const apiKey = useSettings((state) => state.apiKey)
  const model = useSettings((state) => state.model)
  const models = useSettings((state) => allModels(state))
  const [newModel, setNewModel] = useState('')
  const [pulling, setPulling] = useState(false)
  const controllerRef = useRef<AbortController | null>(null)

  useEffect(() => {
    if (!open) {
      return
    }
    return () => {
      controllerRef.current?.abort()
    }
  }, [open])

  if (!open) {
    return null
  }

  const pullModels = async () => {
    if (pulling) {
      return
    }
    if (!apiKey) {
      showToast({ key: 'apiKeyStart' })
      return
    }
    controllerRef.current?.abort()
    const controller = new AbortController()
    controllerRef.current = controller
    setPulling(true)
    try {
      const list = await fetchModelList(apiKey, controller.signal)
      setPulledModels(list)
      showToast({ key: list.length > 0 ? 'modelAdded' : 'noModels' })
    } catch {
      if (!controller.signal.aborted) {
        showToast({ key: 'pullModelsFailed', type: 'error' })
      }
    } finally {
      setPulling(false)
    }
  }

  const addModel = () => {
    const value = newModel.trim()
    if (!value) {
      return
    }
    addCustomModel(value)
    setModel(value)
    setNewModel('')
    showToast({ key: 'modelAdded' })
  }

  const selectModel = (name: string) => {
    setModel(name)
    closeModelPicker()
  }

  return createPortal(
    <div className={styles.overlay} onMouseDown={(event) => event.target === event.currentTarget && closeModelPicker()}>
      <div className={styles.sheet} role="dialog" aria-modal="true" aria-label={t('modelsTitle')}>
        <div className={styles.header}>
          <div className={styles.title}>{t('modelsTitle')}</div>
          <button type="button" className={styles.close} aria-label={t('close')} onClick={closeModelPicker}>
            <IconCloseOutline16 />
          </button>
        </div>

        <div className={styles.addRow}>
          <input
            className={styles.addInput}
            value={newModel}
            placeholder={t('addModelPlaceholder')}
            autoComplete="off"
            spellCheck={false}
            onChange={(event) => setNewModel(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter' && !event.nativeEvent.isComposing) {
                addModel()
              }
            }}
          />
          <button
            type="button"
            className={styles.addButton}
            disabled={!newModel.trim()}
            onClick={addModel}
          >
            {t('addModel')}
          </button>
        </div>

        <div className={styles.pullRow}>
          <button
            type="button"
            className={styles.pullButton}
            disabled={pulling}
            onClick={() => void pullModels()}
          >
            <IconRefreshOutline16 />
            {pulling ? t('pullingModels') : t('pullModels')}
          </button>
        </div>

        {models.length === 0 ? (
          <div className={styles.empty}>{t('noModels')}</div>
        ) : (
          <div className={styles.list}>
            {models.map((name) => (
              <button
                key={name}
                type="button"
                role="option"
                aria-selected={name === model}
                className={clsx(styles.modelButton, name === model && styles.modelSelected)}
                onClick={() => selectModel(name)}
              >
                <span className={styles.modelName}>{name}</span>
                {name === model ? <IconCheckOutline16 /> : null}
              </button>
            ))}
          </div>
        )}
      </div>
    </div>,
    document.body
  )
}
