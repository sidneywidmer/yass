import { Navigation } from "@/components/landing/navigation.tsx";
import { Footer } from "@/components/landing/footer.tsx";
import { Button } from "@/components/ui/button.tsx";
import { useNavigate } from "react-router-dom";
import { Bot, User } from "lucide-react";
import { cn } from "@/lib/utils";
import { useTranslation } from "react-i18next";
import { useSettingsStore } from "@/store/settings.ts";

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
  const { t } = useTranslation();
  const language = useSettingsStore((state) => state.language);

  const publishDate = new Intl.DateTimeFormat(language === "de" ? "de-CH" : "en-US", {
    year: "numeric",
    month: "long",
    day: "numeric"
  }).format(new Date("2024-11-02"));

  const isEnglish = language === "en";
  const works = t("landing.blog.posts.betaRoadmap.works", { returnObjects: true }) as string[];
  const buildSites = t("landing.blog.posts.betaRoadmap.buildSites", { returnObjects: true }) as string[];
  const roadmap = t("landing.blog.posts.betaRoadmap.roadmap", { returnObjects: true }) as string[];
  const future = t("landing.blog.posts.betaRoadmap.future", { returnObjects: true }) as string[];

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
                {t("landing.blog.posts.betaRoadmap.title")}
              </h1>
            </div>

            <div className="space-y-6 text-base">
              {isEnglish && (
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-4">
                  <p className="font-sans text-sm text-blue-900">
                    {t("landing.blog.posts.betaRoadmap.aiDisclaimer")}
                  </p>
                </div>
              )}

              <p
                className="font-sans text-foreground leading-relaxed"
                dangerouslySetInnerHTML={{ __html: t("landing.blog.posts.betaRoadmap.intro") }}
              />

              <div className="pt-2">
                <Button
                  size="lg"
                  onClick={() => navigate("/signup")}
                  className="font-semibold px-8"
                >
                  {t("landing.blog.posts.betaRoadmap.playButton")}
                </Button>
              </div>

              <div className="pt-4">
                <h2 className="font-serif text-2xl font-medium text-foreground mb-4">
                  {t("landing.blog.posts.betaRoadmap.statusTitle")}
                </h2>
                <p className="font-sans text-foreground leading-relaxed mb-4">
                  {t("landing.blog.posts.betaRoadmap.statusP1")}
                </p>
                <p
                  className="font-sans text-foreground leading-relaxed mb-4"
                  dangerouslySetInnerHTML={{ __html: t("landing.blog.posts.betaRoadmap.statusP2") }}
                />
                <p className="font-sans text-foreground leading-relaxed">
                  {t("landing.blog.posts.betaRoadmap.statusP3")}
                </p>
              </div>

              <div>
                <h2 className="font-serif text-2xl font-medium text-foreground mb-4">
                  {t("landing.blog.posts.betaRoadmap.worksTitle")}
                </h2>
                <ul className="space-y-2 list-disc list-outside pl-5 font-sans text-foreground">
                  {works.map((item, idx) => (
                    <li key={idx}>
                      {idx === 1 ? (
                        <>
                          {item.split(" bots oder mit ")[0]} <Bot className="inline h-4 w-4 mx-0.5" /> bots oder mit <User className="inline h-4 w-4 mx-0.5" /> {item.includes("Partner") ? item.split("mit ")[1] : "Partner"}
                        </>
                      ) : idx === 3 ? (
                        <>
                          {item}: <StatusIndicator status="BOT" />, <StatusIndicator status="CONNECTED" />, <StatusIndicator status="DISCONNECTED" />
                        </>
                      ) : (
                        item
                      )}
                    </li>
                  ))}
                </ul>
              </div>

              <div>
                <h2 className="font-serif text-2xl font-medium text-foreground mb-4">
                  {t("landing.blog.posts.betaRoadmap.buildSitesTitle")}
                </h2>
                <ul className="space-y-2 list-disc list-outside pl-5 font-sans text-foreground">
                  {buildSites.map((item, idx) => (
                    <li
                      key={idx}
                      dangerouslySetInnerHTML={{ __html: item }}
                    />
                  ))}
                </ul>
              </div>

              <div>
                <h2 className="font-serif text-2xl font-medium text-foreground mb-4">
                  {t("landing.blog.posts.betaRoadmap.roadmapTitle")}
                </h2>
                <p className="font-sans text-foreground leading-relaxed mb-4">
                  {t("landing.blog.posts.betaRoadmap.roadmapIntro")}
                </p>
                <ul className="space-y-2 list-disc list-outside pl-5 font-sans text-foreground mb-4">
                  {roadmap.map((item, idx) => (
                    <li key={idx}>{item}</li>
                  ))}
                </ul>
              </div>

              <div>
                <h2 className="font-serif text-2xl font-medium text-foreground mb-4">
                  {t("landing.blog.posts.betaRoadmap.futureTitle")}
                </h2>
                <p className="font-sans text-foreground leading-relaxed mb-4">
                  {t("landing.blog.posts.betaRoadmap.futureIntro")}
                </p>
                <ul className="space-y-2 list-disc list-outside pl-5 font-sans text-foreground">
                  {future.map((item, idx) => (
                    <li key={idx}>{item}</li>
                  ))}
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
