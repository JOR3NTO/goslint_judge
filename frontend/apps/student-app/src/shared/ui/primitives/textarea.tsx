import * as React from 'react'

import { cn } from '@/shared/utils/cn'

function Textarea({ className, ...props }: React.ComponentProps<'textarea'>) {
  return (
    <textarea
      data-slot="textarea"
      className={cn(
        'placeholder:text-muted-foreground text-foreground !border-input !bg-background flex field-sizing-content min-h-20 w-full rounded-md border px-3 py-2 text-base shadow-xs transition-[color,box-shadow] outline-none md:text-sm',
        'focus-visible:!border-ring focus-visible:ring-ring/40 focus-visible:ring-2',
        'disabled:!bg-muted disabled:text-muted-foreground disabled:cursor-not-allowed disabled:opacity-60',
        'aria-invalid:!border-destructive aria-invalid:ring-destructive/20',
        className,
      )}
      {...props}
    />
  )
}

export { Textarea }
