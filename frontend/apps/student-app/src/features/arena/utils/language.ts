import type { SubmissionLanguage } from "../types/arena.types"

export const LANGUAGE_OPTIONS: { value: SubmissionLanguage; label: string }[] = [
  { value: "C", label: "C" },
  { value: "CPP", label: "C++" },
  { value: "JAVA", label: "Java" },
  { value: "PYTHON", label: "Python 3" },
]

export const LANGUAGE_LABELS: Record<SubmissionLanguage, string> = {
  C: "C",
  CPP: "C++",
  JAVA: "Java",
  PYTHON: "Python 3",
}

/** Plantilla inicial por lenguaje. Java usa `class Main` (el juez compila Main.java). */
export const LANGUAGE_TEMPLATES: Record<SubmissionLanguage, string> = {
  C: `#include <stdio.h>

int main(void) {

    return 0;
}
`,
  CPP: `#include <iostream>

int main() {
    std::ios::sync_with_stdio(false);
    std::cin.tie(nullptr);

    return 0;
}
`,
  JAVA: `import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

    }
}
`,
  PYTHON: `def main():
    pass


if __name__ == "__main__":
    main()
`,
}
