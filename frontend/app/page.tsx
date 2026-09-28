"use client";

import { useState } from "react";

type TaskItem = {
  task: string;
  assignee: string | null;
  deadline: string | null;
};

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

export default function Home() {
  const [meetingNotes, setMeetingNotes] = useState("");
  const [tasks, setTasks] = useState<TaskItem[] | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const canSubmit = meetingNotes.trim().length > 0 && !loading;

  async function handleSubmit() {
    if (!canSubmit) return;

    setLoading(true);
    setError(null);

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 40000);

    try {
      const response = await fetch(`${API_URL}/api/action-items`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ meetingNotes }),
        signal: controller.signal,
      });

      if (!response.ok) {
        throw new Error(`El servidor respondió con estado ${response.status}`);
      }

      const data = await response.json();
      setTasks(data.tasks ?? []);
    } catch (err) {
      const timedOut = err instanceof DOMException && err.name === "AbortError";
      setError(
        timedOut
          ? "La solicitud tardó demasiado. Intentá de nuevo con un texto más corto."
          : "No pudimos extraer las tareas. Revisá que el backend esté corriendo e intentá de nuevo."
      );
      setTasks(null);
    } finally {
      clearTimeout(timeoutId);
      setLoading(false);
    }
  }

  return (
    <main className="min-h-screen bg-[var(--bg)] text-[var(--text)] flex justify-center px-6 py-16">
      <div className="w-full max-w-2xl">
        <header className="mb-10">
          <p className="text-sm tracking-wide text-[var(--accent-strong)] mb-2">BS</p>
          <h1 className="text-3xl font-semibold mb-3">
            Convierte tus notas de reunión en tareas
          </h1>
          <p className="text-[var(--text-muted)] leading-relaxed">
            Pega el texto de una reunión y te devolvemos, en segundos, la lista de tareas
            con responsable y fecha límite cuando se mencionan.
          </p>
        </header>

        <section className="bg-[var(--surface)] border border-[var(--border)] rounded-xl p-6 mb-8">
          <label htmlFor="meetingNotes" className="block text-sm mb-2 text-[var(--text-muted)]">
            Notas de la reunión
          </label>
          <textarea
            id="meetingNotes"
            value={meetingNotes}
            onChange={(e) => setMeetingNotes(e.target.value)}
            rows={8}
            placeholder="Ej: Juan va a enviar la propuesta al cliente antes del viernes. Maria queda pendiente de revisar el presupuesto..."
            className="w-full bg-[var(--bg)] border border-[var(--border)] rounded-lg p-4 text-[var(--text)] placeholder:text-[var(--text-muted)] focus:outline-none focus:border-[var(--accent)] resize-none"
          />

          <div className="flex items-center justify-between mt-4">
            <p className="text-sm text-[var(--text-muted)]">
              {meetingNotes.trim().length === 0
                ? "Escribí o pega algo de texto para continuar."
                : `${meetingNotes.trim().length} caracteres`}
            </p>
            <button
              onClick={handleSubmit}
              disabled={!canSubmit}
              className="bg-[var(--accent)] hover:bg-[var(--accent-strong)] disabled:opacity-40 disabled:cursor-not-allowed text-[var(--bg)] font-medium px-5 py-2.5 rounded-lg transition-colors"
            >
              {loading ? "Extrayendo..." : "Extraer tareas"}
            </button>
          </div>
        </section>

        {error && (
          <div className="border border-[var(--danger)] text-[var(--danger)] rounded-lg p-4 mb-8 text-sm">
            {error}
          </div>
        )}

        {tasks && tasks.length === 0 && !error && (
          <p className="text-[var(--text-muted)] text-sm">
            No encontramos tareas accionables en ese texto.
          </p>
        )}

        {tasks && tasks.length > 0 && (
          <section>
            <h2 className="text-lg font-semibold mb-4">
              Tareas encontradas ({tasks.length})
            </h2>
            <ul className="space-y-3">
              {tasks.map((t, i) => (
                <li key={i} className="border border-[var(--border)] rounded-lg p-4">
                  <p className="font-medium mb-2">{t.task}</p>
                  <div className="flex flex-wrap gap-x-6 gap-y-1 text-sm text-[var(--text-muted)]">
                    <span>Responsable: {t.assignee ?? "No especificado"}</span>
                    <span>Fecha límite: {t.deadline ?? "No especificado"}</span>
                  </div>
                </li>
              ))}
            </ul>
          </section>
        )}
      </div>
    </main>
  );
}
