import { Zap, Github, Sparkles } from "lucide-react";

export function Features() {
  const features = [
    {
      title: "Early Bird",
      description: "It's a beta, expect bugs but be the first to test and give feedback. Help shape the future of the platform and join a community of early adopters.",
      icon: Zap
    },
    {
      title: "Opensource",
      description: "No ads, no tracking, all opensource. Full transparency and community-driven development with no corporate agenda.",
      icon: Github
    },
    {
      title: "Clean",
      description: "Snappy and clean UI with fast interactions. Enjoy a distraction-free experience designed for speed and simplicity.",
      icon: Sparkles
    }
  ];

  return (
    <div className="w-full border-t border-border">
      <div className="flex w-full flex-col items-center bg-background p-6 md:p-10 md:py-20">
        <div className="flex w-full max-w-6xl flex-col gap-12">
          <div className="text-center max-w-2xl mx-auto">
            <h2 className="font-serif text-4xl md:text-5xl font-medium text-foreground mb-4">
              Features
            </h2>
            <p className="font-sans text-lg text-muted-foreground leading-relaxed">
              Yass is built with players in mind. Experience gaming the way it should be—transparent, fast, and community-driven.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
            {features.map((feature) => {
              const Icon = feature.icon;
              return (
                <div
                  key={feature.title}
                  className="flex flex-col gap-4 p-8 rounded-lg hover:shadow-lg transition-shadow"
                >
                  <Icon className="h-8 w-8 text-foreground" />
                  <h3 className="font-sans text-2xl font-semibold text-foreground">
                    {feature.title}
                  </h3>
                  <p className="font-sans text-base text-muted-foreground leading-relaxed">
                    {feature.description}
                  </p>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
