import { Navigation } from "@/components/landing/navigation.tsx";
import { Footer } from "@/components/landing/footer.tsx";
import { Button } from "@/components/ui/button.tsx";
import { useNavigate } from "react-router-dom";
import { Bot, User } from "lucide-react";
import { cn } from "@/lib/utils";

const StatusIndicator = ({ status }: { status: string }) => {
  return (
    <div className="inline-flex items-center gap-1">
      <div className={cn(
        "w-2 h-2 rounded-full",
        status === "DISCONNECTED" && "bg-red-500",
        status === "BOT" && "bg-blue-500",
        status === "CONNECTED" && "bg-green-500"
      )} />
      <span className="text-xs ml-1">{status === "BOT" ? "Bot" : status === "CONNECTED" ? "Online" : "Offline"}</span>
    </div>
  );
};

export default function BetaRoadmapBlog() {
  const navigate = useNavigate();
  const publishDate = new Intl.DateTimeFormat("de-CH", {
    year: "numeric",
    month: "long",
    day: "numeric"
  }).format(new Date("2024-11-02"));

  return (
    <div className="flex flex-col min-h-screen">
      <Navigation />
      <main className="flex-1 w-full bg-background">
        <div className="flex w-full flex-col items-center p-6 md:p-10 md:py-16">
          <article className="w-full max-w-2xl">
            <div className="mb-8">
              <p className="font-sans text-sm text-muted-foreground mb-4">
                {publishDate}
              </p>
              <h1 className="font-serif text-4xl md:text-5xl font-medium text-foreground mb-6">
                Start der Beta & Roadmap
              </h1>
            </div>

            <div className="space-y-6 text-base">
              <p className="font-sans text-foreground leading-relaxed">
                Es ist soweit! Seit gut <a href="https://github.com/sidneywidmer/yass/commit/f0e905ad5c135d2b0e857b59c376789846668d18" target="_blank" rel="noopener noreferrer">3 Jahren</a> versuche ich immer mal wieder etwas Zeit für «Yass» freizuschaufeln und endlich gibt es eine Version die es Wert ist, der Welt gezeigt zu werden.
              </p>

              <div className="pt-2">
                <Button
                  size="lg"
                  onClick={() => navigate("/signup")}
                  className="font-semibold px-8"
                >
                  Jetzt spielen
                </Button>
              </div>

              <div className="pt-4">
                <h2 className="font-serif text-2xl font-medium text-foreground mb-4">
                  Spielstatus und Betatester
                </h2>
                <p className="font-sans text-foreground leading-relaxed mb-4">
                  Damit der eigentliche Release nicht in einem Debakel endet, bin ich auf deine Hilfe angewiesen. Hilf mit den aktuellen Stand der Applikation zu testen.
                </p>
                <p className="font-sans text-foreground leading-relaxed mb-4">
                  Kurze Anekdote nebenbei: Als ich die App nach 2.5 Jahren Entwicklung zum ersten Mal einem Kollegen gezeigt habe, meinte der nur: «Sidney, du weisst aber schon, dass wir normalerweise im <i>Gegenuhrzeigersinn</i> Jassen, oder?». Ouch!
                </p>
                <p className="font-sans text-foreground leading-relaxed">
                  Falls du einen Fehler findest, kannst du diesen jederzeit direkt melden. TBD. Ob Rechtschreibfehler im Text, Fehler in der Spiellogik oder etwas Unverständliches im User Interface - ich freue mich über jede Meldung.
                </p>
              </div>

              <div>
                <h2 className="font-serif text-2xl font-medium text-foreground mb-4">
                  Was funktioniert
                </h2>
                <ul className="space-y-2 list-disc list-outside pl-5 font-sans text-foreground">
                  <li>Login als Gast oder mit E-Mail und Passwort</li>
                  <li>Spiel erstellen und entweder auf Anzahl Punkt oder Anzahl Hände spielen (gegen <Bot className="inline h-4 w-4 mx-0.5" /> Bots oder mit <User className="inline h-4 w-4 mx-0.5" /> Partner:in)</li>
                  <li>Einem Spiel entweder per QR Code oder mit Code beitreten</li>
                  <li>Die "Präsenz" aller Spieler:innen wird mittels kleinem Kreis angezeigt: <StatusIndicator status="BOT" />, <StatusIndicator status="CONNECTED" />, <StatusIndicator status="DISCONNECTED" /></li>
                  <li>Karten spielen, Weisen, Schieben, Trumpfen, Stöck</li>
                  <li>Aktueller Punktestand anzeigen</li>
                  <li>Analyse Ansicht nach abgeschlossenem Spiel</li>
                </ul>
              </div>

              <div>
                <h2 className="font-serif text-2xl font-medium text-foreground mb-4">
                  Baustellen
                </h2>
                <ul className="space-y-2 list-disc list-outside pl-5 font-sans text-foreground">
                  <li>Die Bots sind absolute Jass-Anfänger. Das liegt einerseits daran, dass ich selber ein nicht wirklich guter Jasser bin und andererseits daran, dass ich erst ein paar <a href="https://github.com/sidneywidmer/yass/blob/master/server/main/kotlin/ch/yass/game/bot/play.kt" target="_blank" rel="noopener noreferrer">wenige Heuristiken</a> umgesetzt habe. Das System ist aber da und kann beliebig erweitert werden.</li>
                  <li>Viele Verbesserungen am Code der die Funktionalität aber nicht gross betrifft (Update Kotlin, Migration weg von `context`, entfernen des legacy "Skin" Systems, Performance Optimierungen, verbessertes Monitoring/Alerting, e.t.c)</li>
                </ul>
              </div>

              <div>
                <h2 className="font-serif text-2xl font-medium text-foreground mb-4">
                  Roadmap
                </h2>
                <p className="font-sans text-foreground leading-relaxed mb-4">
                  Yass ist noch lange nicht da, wo ich es gerne haben würde. Die folgenden Features sind in naher Zukunft geplant:
                </p>
                <ul className="space-y-2 list-disc list-outside pl-5 font-sans text-foreground mb-4">
                  <li>Starker Fokus auf Multiplayer: Automatische Gegnersuche, Rangliste und Spielhistorie</li>
                  <li>Blog: Detaillierte Posts über die Architektur und wie Entwickler selber eine Version von Yass betreiben können</li>
                  <li>Bot Intelligenz: Verbesserte Heuristiken und Monte Carlo Simulation um Bots zu echten Gegnern zu machen</li>
                </ul>
              </div>

              <div>
                <h2 className="font-serif text-2xl font-medium text-foreground mb-4">
                  Zukunftsmusik
                </h2>
                <p className="font-sans text-foreground leading-relaxed mb-4">
                  Ein paar Langzeit Ideen, die so in meinem Kopf herumschwirren:
                </p>
                <ul className="space-y-2 list-disc list-outside pl-5 font-sans text-foreground">
                  <li>Eigene Bots schreiben: Die Möglichkeit für Entwickler, selber einen Bot zu programmieren und gegen andere Bots und echte Spieler zu testen.</li>
                  <li>Turniersystem für Veranstalter, Vereine oder Firmen um einfach online Jassturniere veranstalten zu können</li>
                </ul>
              </div>
            </div>
          </article>
        </div>
      </main>
      <Footer />
    </div>
  );
}
