import { GalleryVerticalEnd, Menu } from "lucide-react";
import { Button } from "@/components/ui/button.tsx";
import { Sheet, SheetContent, SheetTrigger } from "@/components/ui/sheet.tsx";
import { useNavigate } from "react-router-dom";
import { useState } from "react";
import Settings from "@/components/settings.tsx";
import { usePlayerStore } from "@/store/player.ts";

export function Navigation() {
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [settingsOpen, setSettingsOpen] = useState(false);
  const isAuthenticated = usePlayerStore(state => state.isAuthenticated);

  return (
    <>
    <nav className="sticky top-0 z-50 bg-background/80 backdrop-blur-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">
          {/* Logo */}
          <div className="flex-shrink-0 flex items-center gap-2">
            <button
              onClick={() => navigate("/")}
              className="flex items-center gap-2 font-semibold hover:opacity-80 transition-opacity"
            >
              <div className="flex h-6 w-6 items-center justify-center rounded-md bg-primary text-primary-foreground">
                <GalleryVerticalEnd className="size-4" />
              </div>
              <span>Yass</span>
            </button>
          </div>

          {/* Desktop Navigation */}
          <div className="hidden md:flex items-center gap-8">
            {!isAuthenticated && (
              <button
                onClick={() => navigate("/login")}
                className="text-sm text-muted-foreground hover:text-foreground transition-colors"
              >
                Login
              </button>
            )}
            {isAuthenticated && (
              <button
                onClick={() => navigate("/lobby")}
                className="text-sm text-muted-foreground hover:text-foreground transition-colors"
              >
                Lobby
              </button>
            )}
            <button
              onClick={() => setSettingsOpen(true)}
              className="text-sm text-muted-foreground hover:text-foreground transition-colors"
            >
              Settings
            </button>
          </div>

          {/* Mobile Menu Button */}
          <div className="md:hidden">
            <Sheet open={mobileMenuOpen} onOpenChange={setMobileMenuOpen}>
              <SheetTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  className="relative"
                >
                  <Menu className="h-5 w-5" />
                </Button>
              </SheetTrigger>
              <SheetContent side="right" className="w-64">
                <div className="flex flex-col gap-6 mt-8">
                  <div className="flex flex-col gap-2">
                    {!isAuthenticated && (
                      <button
                        onClick={() => {
                          navigate("/login");
                          setMobileMenuOpen(false);
                        }}
                        className="text-left text-sm font-medium hover:text-primary transition-colors"
                      >
                        Login
                      </button>
                    )}
                    {isAuthenticated && (
                      <button
                        onClick={() => {
                          navigate("/lobby");
                          setMobileMenuOpen(false);
                        }}
                        className="text-left text-sm font-medium hover:text-primary transition-colors"
                      >
                        Lobby
                      </button>
                    )}
                    <button
                      onClick={() => {
                        setSettingsOpen(true);
                        setMobileMenuOpen(false);
                      }}
                      className="text-left text-sm font-medium hover:text-primary transition-colors"
                    >
                      Settings
                    </button>
                  </div>
                </div>
              </SheetContent>
            </Sheet>
          </div>
        </div>
      </div>
    </nav>
    <Settings
      triggerVariant="none"
      open={settingsOpen}
      onOpenChange={setSettingsOpen}
    />
    </>
  );
}
