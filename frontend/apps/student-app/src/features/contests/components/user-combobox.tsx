"use client"

import { useState } from "react"
import { Check, ChevronsUpDown, Loader2 } from "lucide-react"
import { Button } from "@/shared/ui/primitives/button"
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/shared/ui/primitives/command"
import { Popover, PopoverContent, PopoverTrigger } from "@/shared/ui/primitives/popover"
import { cn } from "@/shared/utils/cn"
import { useUserSearch } from "../hooks/use-user-search"
import type { UserSummaryDTO } from "../types/contest.types"

interface UserComboboxProps {
  id?: string
  value: UserSummaryDTO | null
  onChange: (user: UserSummaryDTO | null) => void
  disabled?: boolean
  invalid?: boolean
}

/** Desplegable con buscador integrado para elegir un usuario por su username. */
export function UserCombobox({ id, value, onChange, disabled, invalid }: UserComboboxProps) {
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState("")
  const { users, loading, failed } = useUserSearch(query, open)

  return (
    <Popover
      open={open}
      onOpenChange={(next) => {
        setOpen(next)
        if (!next) setQuery("")
      }}
    >
      <PopoverTrigger asChild>
        <Button
          id={id}
          type="button"
          variant="outline"
          role="combobox"
          aria-expanded={open}
          aria-invalid={invalid}
          disabled={disabled}
          className="w-full justify-between border-border bg-input font-normal"
        >
          <span className={cn("truncate", !value && "text-muted-foreground")}>
            {value ? value.username : "Selecciona un estudiante"}
          </span>
          <ChevronsUpDown className="h-4 w-4 shrink-0 opacity-50" />
        </Button>
      </PopoverTrigger>
      <PopoverContent align="start" className="w-[var(--radix-popover-trigger-width)] p-0">
        {/* El filtrado lo hace el backend: cmdk solo muestra lo que llega. */}
        <Command shouldFilter={false}>
          <CommandInput placeholder="Buscar por nombre de usuario…" value={query} onValueChange={setQuery} />
          <CommandList>
            {loading ? (
              <div className="flex items-center justify-center gap-2 py-6 text-sm text-muted-foreground">
                <Loader2 className="h-4 w-4 animate-spin" /> Buscando…
              </div>
            ) : failed ? (
              <p className="py-6 text-center text-sm text-destructive">No se pudo cargar la lista.</p>
            ) : (
              <>
                <CommandEmpty>No se encontraron usuarios.</CommandEmpty>
                <CommandGroup>
                  {users.map((user) => (
                    <CommandItem
                      key={user.id}
                      value={user.id}
                      onSelect={() => {
                        onChange(user)
                        setOpen(false)
                      }}
                    >
                      <Check className={cn("h-4 w-4", value?.id === user.id ? "opacity-100" : "opacity-0")} />
                      {user.username}
                    </CommandItem>
                  ))}
                </CommandGroup>
              </>
            )}
          </CommandList>
        </Command>
      </PopoverContent>
    </Popover>
  )
}
