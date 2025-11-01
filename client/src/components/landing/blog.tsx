import { ArrowRight } from "lucide-react";

export function Blog() {
  const posts = [
    {
      title: "Humble Beginnings",
      date: "November 1, 2024",
      abstract: "Learn about how Yass started as a passion project and the vision behind building a modern webclient for card gaming. Discover the story of why we decided to create an open-source alternative that puts players first.",
      slug: "humble-beginnings"
    },
    {
      title: "Architecture Overview",
      date: "October 28, 2024",
      abstract: "Dive into the technical architecture that powers Yass. From real-time WebSocket communication to our database design, explore how we built a scalable and performant platform for multiplayer card gaming.",
      slug: "architecture-overview"
    }
  ];

  return (
    <div className="w-full border-t border-border">
      <div className="flex w-full flex-col items-center bg-background p-6 md:p-10 md:py-20">
        <div className="flex w-full max-w-6xl flex-col gap-12">
          <div className="text-center max-w-2xl mx-auto">
            <h2 className="font-serif text-4xl md:text-5xl font-medium text-foreground mb-4">
              Blog
            </h2>
            <p className="font-sans text-lg text-muted-foreground leading-relaxed">
              Stay updated with the latest news, insights, and behind-the-scenes stories from the Yass community.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
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
                  Read more
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
