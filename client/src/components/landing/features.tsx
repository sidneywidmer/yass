import {Bird, Github, Zap} from "lucide-react";
import {useTranslation} from "react-i18next";

export function Features() {
  const {t} = useTranslation();
  const features = [
    {
      title: t("landing.features.earlyBird.title"),
      description: t("landing.features.earlyBird.description"),
      icon: Bird
    },
    {
      title: t("landing.features.noAds.title"),
      description: t("landing.features.noAds.description", {interpolation: {escapeValue: false}}),
      icon: Github
    },
    {
      title: t("landing.features.fastSimple.title"),
      description: t("landing.features.fastSimple.description"),
      icon: Zap
    }
  ];

  return (
    <div className="w-full border-t border-border">
      <div className="flex w-full flex-col items-center bg-background p-6 pt-10 md:p-10 md:py-20">
        <div className="flex w-full max-w-6xl flex-col gap-8 md:gap-12">
          <div className="text-center max-w-2xl mx-auto">
            <h2 className="font-serif text-4xl md:text-5xl font-medium text-foreground mb-4">
              {t("landing.features.title")}
            </h2>
            <p className="font-sans text-lg text-muted-foreground leading-relaxed">
              {t("landing.features.subtitle")}
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 md:gap-8">
            {features.map((feature) => {
              const Icon = feature.icon;
              return (
                <div
                  key={feature.title}
                  className="flex flex-col gap-4 p-8 rounded-lg hover:shadow-lg transition-shadow"
                >
                  <Icon className="h-8 w-8 text-foreground"/>
                  <h3 className="font-sans text-2xl font-semibold text-foreground">
                    {feature.title}
                  </h3>
                  <p
                    className="font-sans text-base text-muted-foreground leading-relaxed"
                    dangerouslySetInnerHTML={{ __html: feature.description }}
                  />
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
