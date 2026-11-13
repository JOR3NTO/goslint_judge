"use client"

import { useCallback, useMemo, useState } from "react"
import Link from "next/link"
import { useParams } from "next/navigation"
import { Navbar } from "@/shared/ui/navbar"
import { Footer } from "@/shared/ui/footer"
import { Button } from "@/shared/ui/primitives/button"
import { Skeleton } from "@/shared/ui/primitives/skeleton"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/shared/ui/primitives/tabs"
import { ArrowLeft, FileCode2, History, Lock, MessageSquare, Trophy } from "lucide-react"
import { useArena } from "../hooks/use-arena"
import { useProblemDetail } from "../hooks/use-problem-detail"
import { useSubmissions } from "../hooks/use-submissions"
import { teamProblemStatus } from "../utils/verdict"
import { ArenaHeader } from "./arena-header"
import { ArenaMessage, ComingSoon } from "./arena-message"
import { ClarificationsTab } from "./clarifications-tab"
import { ProblemsTab } from "./problems-tab"
import { RankingTab } from "./ranking-tab"
import { SubmissionDetailDialog } from "./submission-detail-dialog"
import { SubmissionsTab } from "./submissions-tab"

type ArenaTab = "problems" | "clarifications" | "submissions" | "ranking"

const tabTriggerClass =
  "data-[state=active]:bg-primary data-[state=active]:text-primary-foreground"

/** Página de la arena de una maratón: solo para equipos inscritos. */
export function ContestArenaPage() {
  const { id } = useParams<{ id: string }>()
  const arena = useArena(id)
  const teamId = arena.team?.id ?? null
  const subs = useSubmissions(teamId)

  const [tab, setTab] = useState<ArenaTab>("problems")
  const [selectedProblemId, setSelectedProblemId] = useState<string | null>(null)
  // Se guarda el id (no la copia del envío) para que el diálogo refleje la versión
  // actual del listado. El id se conserva al cerrar para que el contenido no
  // desaparezca durante la animación de salida.
  const [detailSubmissionId, setDetailSubmissionId] = useState<string | null>(null)
  const [detailOpen, setDetailOpen] = useState(false)

  // Si aún no hay selección (o el problema ya no existe), se muestra el primero.
  const selected =
    arena.problems.find((p) => p.problemId === selectedProblemId) ?? arena.problems[0] ?? null
  const detail = useProblemDetail(selected?.problemId ?? null)

  const problemsById = useMemo(
    () => new Map(arena.problems.map((p) => [p.problemId, p])),
    [arena.problems]
  )
  const statusOf = useCallback(
    (problemId: string) => teamProblemStatus(subs.submissions, problemId),
    [subs.submissions]
  )
  const accepted = arena.problems.filter((p) => statusOf(p.problemId).kind === "accepted").length

  const contest = arena.contest
  const canSubmit = contest?.status === "ACTIVE" && teamId !== null
  const disabledReason =
    contest?.status === "UPCOMING"
      ? "Aún no inicia. Podrás enviar soluciones cuando comience la maratón."
      : contest?.status === "ENDED"
        ? "Finalizó. Puedes revisar los problemas y tus envíos, pero ya no se aceptan soluciones."
        : null

  const detailSubmission = subs.submissions.find((s) => s.id === detailSubmissionId) ?? null
  const detailLabel = detailSubmission
    ? problemsById.get(detailSubmission.problemId)?.letter ?? "problema"
    : ""

  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main className="pt-16">
        <div className="mx-auto max-w-7xl px-4 pt-6 sm:px-6 lg:px-8">
          <Link
            href={`/contests/${id}`}
            className="inline-flex items-center gap-2 text-muted-foreground transition-colors hover:text-foreground"
          >
            <ArrowLeft className="h-4 w-4" />
            Volver al detalle de la maratón
          </Link>
        </div>

        {arena.isLoading ? (
          <div className="mx-auto max-w-7xl space-y-4 px-4 py-6 sm:px-6 lg:px-8">
            <Skeleton className="h-10 w-2/3" />
            <Skeleton className="h-4 w-1/3" />
            <Skeleton className="h-96 w-full" />
          </div>
        ) : arena.error ? (
          <div className="px-4 py-12 sm:px-6">
            <ArenaMessage
              icon={<Lock className="h-6 w-6" />}
              title="No se pudo abrir la arena"
              description={arena.error}
              actions={
                <Button variant="outline" onClick={arena.reload}>
                  Reintentar
                </Button>
              }
            />
          </div>
        ) : !arena.team || !contest ? (
          <div className="px-4 py-12 sm:px-6">
            <ArenaMessage
              icon={<Lock className="h-6 w-6" />}
              title="No estás inscrito en esta maratón"
              description="Solo los equipos inscritos pueden entrar a la arena. Inscríbete desde el detalle de la maratón."
              actions={
                <Link href={`/contests/${id}`}>
                  <Button className="bg-primary text-primary-foreground hover:bg-primary/90">
                    Ir al detalle de la maratón
                  </Button>
                </Link>
              }
            />
          </div>
        ) : (
          <>
            <ArenaHeader contest={contest} accepted={accepted} total={arena.problems.length} />

            <section className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
              {arena.problemsFailed && (
                <p className="mb-4 rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-foreground">
                  No se pudieron cargar los problemas de la maratón.
                </p>
              )}

              <Tabs value={tab} onValueChange={(value) => setTab(value as ArenaTab)} className="space-y-6">
                <TabsList className="h-auto flex-wrap border border-border bg-secondary/50">
                  <TabsTrigger value="problems" className={tabTriggerClass}>
                    <FileCode2 className="mr-2 h-4 w-4" />
                    Problemas
                  </TabsTrigger>
                  <TabsTrigger value="clarifications" className={tabTriggerClass}>
                    <MessageSquare className="mr-2 h-4 w-4" />
                    Aclaraciones
                  </TabsTrigger>
                  <TabsTrigger value="submissions" className={tabTriggerClass}>
                    <History className="mr-2 h-4 w-4" />
                    Mis envíos
                  </TabsTrigger>
                  <TabsTrigger value="ranking" className={tabTriggerClass}>
                    <Trophy className="mr-2 h-4 w-4" />
                    Ranking
                  </TabsTrigger>
                </TabsList>

                <TabsContent value="problems">
                  <ProblemsTab
                    problems={arena.problems}
                    selected={selected}
                    statusOf={statusOf}
                    onSelect={(problemId) => setSelectedProblemId(problemId)}
                    detail={detail}
                    submit={{
                      submissions: subs.submissions,
                      canSubmit,
                      disabledReason,
                      onSubmit: (language, code) =>
                        subs.submit(selected?.problemId ?? "", language, code),
                      onViewAll: () => setTab("submissions"),
                    }}
                  />
                </TabsContent>

                <TabsContent value="clarifications">
                  <ClarificationsTab
                    contestId={id}
                    problems={arena.problems}
                    myTeamId={teamId}
                    initialProblemId={selected?.id ?? null}
                  />
                </TabsContent>

                <TabsContent value="submissions">
                  <SubmissionsTab
                    submissions={subs.submissions}
                    problemsById={problemsById}
                    isLoading={subs.isLoading}
                    error={subs.error}
                    onSelect={(submissionId) => {
                      setDetailSubmissionId(submissionId)
                      setDetailOpen(true)
                    }}
                  />
                </TabsContent>

                <TabsContent value="ranking">
                  <RankingTab
                    contestId={id}
                    contestActive={contest.status === "ACTIVE"}
                    myTeamId={teamId}
                  />
                </TabsContent>
              </Tabs>
            </section>
          </>
        )}
      </main>

      <SubmissionDetailDialog
        submission={detailSubmission}
        problemLabel={detailLabel}
        open={detailOpen && detailSubmission !== null}
        onOpenChange={setDetailOpen}
      />

      <Footer />
    </div>
  )
}
