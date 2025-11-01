import { ArrowRight } from "lucide-react";
import { useTranslation } from "react-i18next";

export function Blog() {
  const { t } = useTranslation();
  const posts = [
    {
      title: t("landing.blog.posts.humbleBeginnings.title"),
      date: "November 1, 2024",
      abstract: t("landing.blog.posts.humbleBeginnings.abstract"),
      slug: "humble-beginnings"
    },
    {
      title: t("landing.blog.posts.architecture.title"),
      date: "October 28, 2024",
      abstract: t("landing.blog.posts.architecture.abstract"),
      slug: "architecture-overview"
    }
  ];

  return (
    <div className="w-full border-t border-border">
      <div className="flex w-full flex-col items-center bg-background p-6 pt-10 md:p-10 md:py-20">
        <div className="flex w-full max-w-6xl flex-col gap-8 md:gap-16">
          <div className="text-center max-w-2xl mx-auto">
            <h2 className="font-serif text-4xl md:text-5xl font-medium text-foreground mb-4">
              {t("landing.blog.title")}
            </h2>
            <p className="font-sans text-lg text-muted-foreground leading-relaxed">
              {t("landing.blog.subtitle")}
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 md:gap-8">
            {posts.map((post) => (
              <div
                key={post.slug}
                className="flex flex-col gap-4 pb-6 hover:opacity-75 transition-opacity"
              >
                <div>
                  <p className="font-sans text-sm text-muted-foreground mb-2">
                    {post.date}
                  </p>
                  <h3 className="font-sans text-2xl font-semibold text-foreground mb-3">
                    {post.title}
                  </h3>
                </div>
                <p className="font-sans text-base text-muted-foreground leading-relaxed flex-grow">
                  {post.abstract}
                </p>
                <a
                  href={`/blog/${post.slug}`}
                  className="font-sans text-base font-medium text-foreground hover:text-primary transition-colors flex items-center gap-2 pt-2"
                >
                  {t("landing.blog.readMore")}
                  <ArrowRight className="h-4 w-4" />
                </a>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
