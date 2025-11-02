import { Navigation } from "@/components/landing/navigation.tsx";
import { Header } from "@/components/landing/header.tsx";
import { Features } from "@/components/landing/features.tsx";
import { Blog } from "@/components/landing/blog.tsx";
import { Footer } from "@/components/landing/footer.tsx";

export default function Landing() {
  return (
    <>
      <Navigation />
      <Header />
      <Features />
      <Blog />
      <Footer />
    </>
  );
}
