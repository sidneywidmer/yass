import { Heart } from "lucide-react";

export function Footer() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="w-full bg-muted">
      <div className="flex w-full flex-col items-center justify-center p-6 md:p-10 md:py-16">
        <div className="flex flex-col items-center gap-4 text-center">
          <div className="flex items-center gap-2 justify-center">
            <p className="font-sans text-base text-foreground">
              Made with
            </p>
            <Heart className="h-4 w-4 text-foreground fill-current" />
            <p className="font-sans text-base text-foreground">
              in Switzerland
            </p>
          </div>
          <p className="font-sans text-sm text-muted-foreground">
            © {currentYear} Yass. All rights reserved.
          </p>
        </div>
      </div>
    </footer>
  );
}
