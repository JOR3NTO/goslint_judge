import * as React from 'react'

import { cn } from '@/shared/utils/cn'

function Input({ className, type, ...props }: React.ComponentProps<'input'>) {
  return (
    <input
      type={type}
      data-slot="input"
      className={cn(
        // Base: fondo blanco explícito (!important para ganar a bg-input que pasan algunas vistas), borde visible, texto oscuro, altura cómoda
        'file:text-foreground placeholder:text-muted-foreground selection:bg-primary selection:text-primary-foreground !bg-background text-foreground !border-input h-10 w-full min-w-0 rounded-md border px-3 py-2 text-base shadow-xs transition-[color,box-shadow] outline-none file:inline-flex file:h-7 file:border-0 file:bg-transparent file:text-sm file:font-medium md:text-sm',
        // Foco: borde verde + anillo verde visible
        'focus-visible:!border-ring focus-visible:ring-ring/40 focus-visible:ring-2',
        // Deshabilitado: claramente atenuado
        'disabled:!bg-muted disabled:text-muted-foreground disabled:cursor-not-allowed disabled:opacity-60',
        // Error
        'aria-invalid:!border-destructive aria-invalid:ring-destructive/20',
        className,
      )}
      {...props}
    />
  )
}

export { Input }
