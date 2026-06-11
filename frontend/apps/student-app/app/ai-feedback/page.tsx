"use client"

import { useState } from "react"
import { Navbar } from "@/components/navbar"
import { Footer } from "@/components/footer"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Textarea } from "@/components/ui/textarea"
import { Badge } from "@/components/ui/badge"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { 
  Sparkles, 
  Code2, 
  Zap, 
  CheckCircle2,
  AlertTriangle,
  Lightbulb,
  Copy,
  RotateCcw,
  Send,
  Clock,
  Cpu
} from "lucide-react"

const exampleCode = `#include <bits/stdc++.h>
using namespace std;

int main() {
    int n;
    cin >> n;
    vector<int> arr(n);
    
    for(int i = 0; i < n; i++) {
        cin >> arr[i];
    }
    
    // Bubble sort
    for(int i = 0; i < n; i++) {
        for(int j = 0; j < n-1; j++) {
            if(arr[j] > arr[j+1]) {
                swap(arr[j], arr[j+1]);
            }
        }
    }
    
    for(int i = 0; i < n; i++) {
        cout << arr[i] << " ";
    }
    
    return 0;
}`

const languages = [
  { value: "cpp", label: "C++" },
  { value: "python", label: "Python" },
  { value: "java", label: "Java" },
  { value: "javascript", label: "JavaScript" },
  { value: "go", label: "Go" },
  { value: "rust", label: "Rust" },
]

interface Feedback {
  type: "optimization" | "bug" | "suggestion" | "complexity"
  title: string
  description: string
  lineNumbers?: string
  improvedCode?: string
}

const mockFeedback: Feedback[] = [
  {
    type: "complexity",
    title: "Complejidad Temporal Subóptima",
    description: "Tu algoritmo de ordenamiento tiene complejidad O(n²). Para entradas grandes (n > 10,000), esto será demasiado lento. Considera usar sort() de la STL que tiene complejidad O(n log n).",
    lineNumbers: "líneas 13-19"
  },
  {
    type: "optimization",
    title: "Uso de sort() de STL",
    description: "Puedes reemplazar el bubble sort con sort(arr.begin(), arr.end()) para obtener mejor rendimiento.",
    improvedCode: `#include <bits/stdc++.h>
using namespace std;

int main() {
    ios_base::sync_with_stdio(false);
    cin.tie(NULL);
    
    int n;
    cin >> n;
    vector<int> arr(n);
    
    for(int i = 0; i < n; i++) {
        cin >> arr[i];
    }
    
    sort(arr.begin(), arr.end());
    
    for(int i = 0; i < n; i++) {
        cout << arr[i] << " ";
    }
    
    return 0;
}`
  },
  {
    type: "suggestion",
    title: "Optimización de I/O",
    description: "Agrega ios_base::sync_with_stdio(false) y cin.tie(NULL) para mejorar la velocidad de entrada/salida.",
    lineNumbers: "después de int main() {"
  },
  {
    type: "bug",
    title: "Posible Optimización del Loop Interno",
    description: "El loop interno puede optimizarse a n-i-1 en cada iteración ya que los elementos más grandes ya están en su posición correcta.",
    lineNumbers: "línea 14"
  }
]

const feedbackTypeConfig = {
  complexity: {
    icon: Clock,
    color: "text-orange-400",
    bgColor: "bg-orange-500/10",
    borderColor: "border-orange-500/30"
  },
  optimization: {
    icon: Zap,
    color: "text-primary",
    bgColor: "bg-primary/10",
    borderColor: "border-primary/30"
  },
  suggestion: {
    icon: Lightbulb,
    color: "text-blue-400",
    bgColor: "bg-blue-500/10",
    borderColor: "border-blue-500/30"
  },
  bug: {
    icon: AlertTriangle,
    color: "text-red-400",
    bgColor: "bg-red-500/10",
    borderColor: "border-red-500/30"
  }
}

export default function AIFeedbackPage() {
  const [code, setCode] = useState(exampleCode)
  const [language, setLanguage] = useState("cpp")
  const [isAnalyzing, setIsAnalyzing] = useState(false)
  const [feedback, setFeedback] = useState<Feedback[] | null>(null)
  const [copiedIndex, setCopiedIndex] = useState<number | null>(null)

  const handleAnalyze = () => {
    setIsAnalyzing(true)
    setFeedback(null)
    
    // Simulate AI analysis
    setTimeout(() => {
      setFeedback(mockFeedback)
      setIsAnalyzing(false)
    }, 2500)
  }

  const handleCopy = (text: string, index: number) => {
    navigator.clipboard.writeText(text)
    setCopiedIndex(index)
    setTimeout(() => setCopiedIndex(null), 2000)
  }

  const handleReset = () => {
    setCode("")
    setFeedback(null)
  }

  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <main className="pt-16">
        {/* Header */}
        <section className="relative py-16 border-b border-border">
          <div className="absolute inset-0 bg-gradient-to-b from-primary/5 to-transparent" />
          <div className="relative mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="text-center">
              <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-primary/10 border border-primary/30 text-primary text-sm font-medium mb-6">
                <Sparkles className="h-4 w-4" />
                <span>Powered by AI</span>
              </div>
              <h1 className="text-4xl sm:text-5xl font-bold text-foreground mb-4">
                AI <span className="text-primary text-glow">Feedback</span>
              </h1>
              <p className="text-lg text-muted-foreground max-w-2xl mx-auto">
                Obtén retroalimentación instantánea sobre tu código. Nuestro sistema de IA 
                analiza tu solución y sugiere optimizaciones, detecta bugs y mejora tu código.
              </p>
            </div>
          </div>
        </section>

        {/* Main Content */}
        <section className="py-12">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
              {/* Code Input */}
              <div className="space-y-4">
                <Card className="border-border bg-card">
                  <CardHeader className="pb-4">
                    <div className="flex items-center justify-between">
                      <div>
                        <CardTitle className="text-foreground flex items-center gap-2">
                          <Code2 className="h-5 w-5 text-primary" />
                          Tu Código
                        </CardTitle>
                        <CardDescription>
                          Pega tu código aquí para obtener feedback
                        </CardDescription>
                      </div>
                      <Select value={language} onValueChange={setLanguage}>
                        <SelectTrigger className="w-32 bg-input border-border">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent className="bg-popover border-border">
                          {languages.map((lang) => (
                            <SelectItem key={lang.value} value={lang.value}>
                              {lang.label}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <div className="relative">
                      <Textarea
                        value={code}
                        onChange={(e) => setCode(e.target.value)}
                        placeholder="// Pega tu código aquí..."
                        className="min-h-[400px] font-mono text-sm bg-[oklch(0.08_0.01_240)] border-border focus:border-primary resize-none"
                      />
                      <div className="absolute top-3 right-3 flex gap-2">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={handleReset}
                          className="h-8 text-muted-foreground hover:text-foreground"
                        >
                          <RotateCcw className="h-4 w-4" />
                        </Button>
                      </div>
                    </div>
                    <div className="flex items-center justify-between mt-4">
                      <p className="text-sm text-muted-foreground">
                        {code.split('\n').length} líneas
                      </p>
                      <Button
                        onClick={handleAnalyze}
                        disabled={!code.trim() || isAnalyzing}
                        className="gap-2 bg-primary text-primary-foreground hover:bg-primary/90 glow-green-sm"
                      >
                        {isAnalyzing ? (
                          <>
                            <Cpu className="h-4 w-4 animate-spin" />
                            Analizando...
                          </>
                        ) : (
                          <>
                            <Send className="h-4 w-4" />
                            Analizar Código
                          </>
                        )}
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              </div>

              {/* Feedback Output */}
              <div className="space-y-4">
                <Card className="border-border bg-card">
                  <CardHeader className="pb-4">
                    <CardTitle className="text-foreground flex items-center gap-2">
                      <Sparkles className="h-5 w-5 text-primary" />
                      Retroalimentación
                    </CardTitle>
                    <CardDescription>
                      Sugerencias y optimizaciones para tu código
                    </CardDescription>
                  </CardHeader>
                  <CardContent>
                    {isAnalyzing ? (
                      <div className="flex flex-col items-center justify-center py-16 space-y-4">
                        <div className="relative">
                          <div className="h-16 w-16 rounded-full border-4 border-primary/30 border-t-primary animate-spin" />
                          <Sparkles className="h-6 w-6 text-primary absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2" />
                        </div>
                        <p className="text-muted-foreground">Analizando tu código...</p>
                        <p className="text-sm text-muted-foreground/70">
                          Revisando complejidad, bugs y optimizaciones
                        </p>
                      </div>
                    ) : feedback ? (
                      <div className="space-y-4">
                        {/* Summary */}
                        <div className="flex items-center gap-4 p-4 rounded-lg bg-primary/10 border border-primary/30">
                          <CheckCircle2 className="h-8 w-8 text-primary" />
                          <div>
                            <p className="font-medium text-foreground">Análisis Completado</p>
                            <p className="text-sm text-muted-foreground">
                              Encontramos {feedback.length} sugerencias para mejorar tu código
                            </p>
                          </div>
                        </div>

                        {/* Feedback Items */}
                        <div className="space-y-3 max-h-[500px] overflow-y-auto pr-2">
                          {feedback.map((item, index) => {
                            const config = feedbackTypeConfig[item.type]
                            const Icon = config.icon
                            
                            return (
                              <div
                                key={index}
                                className={`p-4 rounded-lg border ${config.bgColor} ${config.borderColor}`}
                              >
                                <div className="flex items-start gap-3">
                                  <Icon className={`h-5 w-5 ${config.color} mt-0.5`} />
                                  <div className="flex-1 space-y-2">
                                    <div className="flex items-start justify-between gap-2">
                                      <h4 className="font-medium text-foreground">
                                        {item.title}
                                      </h4>
                                      {item.lineNumbers && (
                                        <Badge variant="outline" className="text-xs border-border">
                                          {item.lineNumbers}
                                        </Badge>
                                      )}
                                    </div>
                                    <p className="text-sm text-muted-foreground">
                                      {item.description}
                                    </p>
                                    {item.improvedCode && (
                                      <div className="mt-3">
                                        <div className="flex items-center justify-between mb-2">
                                          <span className="text-xs text-muted-foreground">
                                            Código mejorado:
                                          </span>
                                          <Button
                                            variant="ghost"
                                            size="sm"
                                            onClick={() => handleCopy(item.improvedCode!, index)}
                                            className="h-7 text-xs gap-1"
                                          >
                                            {copiedIndex === index ? (
                                              <>
                                                <CheckCircle2 className="h-3 w-3" />
                                                Copiado
                                              </>
                                            ) : (
                                              <>
                                                <Copy className="h-3 w-3" />
                                                Copiar
                                              </>
                                            )}
                                          </Button>
                                        </div>
                                        <pre className="p-3 rounded bg-[oklch(0.08_0.01_240)] border border-border overflow-x-auto text-xs font-mono text-muted-foreground">
                                          {item.improvedCode}
                                        </pre>
                                      </div>
                                    )}
                                  </div>
                                </div>
                              </div>
                            )
                          })}
                        </div>
                      </div>
                    ) : (
                      <div className="flex flex-col items-center justify-center py-16 space-y-4">
                        <div className="h-16 w-16 rounded-full bg-secondary flex items-center justify-center">
                          <Code2 className="h-8 w-8 text-muted-foreground" />
                        </div>
                        <p className="text-muted-foreground text-center">
                          Pega tu código y haz clic en &ldquo;Analizar Código&rdquo; para obtener 
                          retroalimentación impulsada por IA.
                        </p>
                      </div>
                    )}
                  </CardContent>
                </Card>
              </div>
            </div>

            {/* Features */}
            <div className="mt-16 grid grid-cols-1 md:grid-cols-3 gap-6">
              {[
                {
                  icon: Clock,
                  title: "Análisis de Complejidad",
                  description: "Detectamos si tu algoritmo tiene una complejidad temporal adecuada para el problema."
                },
                {
                  icon: Zap,
                  title: "Optimizaciones",
                  description: "Sugerencias para hacer tu código más eficiente y rápido."
                },
                {
                  icon: AlertTriangle,
                  title: "Detección de Bugs",
                  description: "Identificamos posibles errores y edge cases que podrías estar ignorando."
                }
              ].map((feature, i) => (
                <Card key={i} className="border-border bg-card/50">
                  <CardContent className="pt-6">
                    <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary/10 border border-primary/30 mb-4">
                      <feature.icon className="h-6 w-6 text-primary" />
                    </div>
                    <h3 className="font-semibold text-foreground mb-2">{feature.title}</h3>
                    <p className="text-sm text-muted-foreground">{feature.description}</p>
                  </CardContent>
                </Card>
              ))}
            </div>
          </div>
        </section>
      </main>
      <Footer />
    </div>
  )
}
