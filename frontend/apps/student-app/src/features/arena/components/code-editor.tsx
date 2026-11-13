"use client"

import { useLayoutEffect, useRef, type KeyboardEvent, type UIEvent } from "react"

/** Cuatro espacios por cada Tab. */
const INDENT = "    "
/** Alto fijo compartido por el gutter de números de línea y el textarea. */
const EDITOR_HEIGHT = "360px"

interface CodeEditorProps {
  id?: string
  value: string
  onChange: (value: string) => void
  disabled?: boolean
  ariaLabel?: string
}

/**
 * Editor sencillo: un textarea monoespaciado con números de línea a la izquierda.
 * Tab inserta 4 espacios (en lugar de mover el foco). Sin dependencias externas.
 */
export function CodeEditor({ id, value, onChange, disabled, ariaLabel }: CodeEditorProps) {
  const textareaRef = useRef<HTMLTextAreaElement>(null)
  const gutterRef = useRef<HTMLDivElement>(null)
  /** Posición del cursor a restaurar tras un Tab (el valor controlado la mueve al final). */
  const pendingCaret = useRef<number | null>(null)

  const lineCount = value.split("\n").length

  useLayoutEffect(() => {
    if (pendingCaret.current !== null && textareaRef.current) {
      textareaRef.current.setSelectionRange(pendingCaret.current, pendingCaret.current)
      pendingCaret.current = null
    }
  }, [value])

  const handleKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key !== "Tab" || event.shiftKey || disabled) return
    event.preventDefault()
    const { selectionStart, selectionEnd } = event.currentTarget
    pendingCaret.current = selectionStart + INDENT.length
    onChange(value.slice(0, selectionStart) + INDENT + value.slice(selectionEnd))
  }

  // Mantiene los números de línea alineados con el contenido al hacer scroll.
  const handleScroll = (event: UIEvent<HTMLTextAreaElement>) => {
    if (gutterRef.current) gutterRef.current.scrollTop = event.currentTarget.scrollTop
  }

  return (
    <div className="flex overflow-hidden rounded-md border border-border bg-muted/40 font-mono text-sm shadow-sm focus-within:ring-2 focus-within:ring-ring/50">
      <div
        ref={gutterRef}
        aria-hidden="true"
        className="w-12 shrink-0 select-none overflow-hidden border-r border-border bg-muted py-3 pr-3 text-right leading-6 text-muted-foreground"
        style={{ height: EDITOR_HEIGHT }}
      >
        {Array.from({ length: lineCount }, (_, index) => (
          <div key={index}>{index + 1}</div>
        ))}
      </div>
      <textarea
        ref={textareaRef}
        id={id}
        aria-label={ariaLabel ?? "Código fuente"}
        value={value}
        disabled={disabled}
        onChange={(event) => onChange(event.target.value)}
        onKeyDown={handleKeyDown}
        onScroll={handleScroll}
        spellCheck={false}
        autoCapitalize="off"
        autoCorrect="off"
        wrap="off"
        className="w-full min-w-0 resize-none overflow-auto whitespace-pre bg-transparent p-3 leading-6 text-foreground caret-primary outline-none selection:bg-primary/25 selection:text-foreground disabled:opacity-60"
        style={{ height: EDITOR_HEIGHT }}
      />
    </div>
  )
}
