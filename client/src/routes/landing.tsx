import { Navigation } from "@/components/navigation.tsx";

export default function Landing() {
  return (
    <>
      <Navigation />
      <div className="flex min-h-[calc(100vh-4rem)] flex-col items-center justify-center gap-6 bg-muted p-6 md:p-10">
        <div className="flex w-full max-w-sm flex-col gap-6">
          <div className="flex flex-col gap-4 rounded-lg border bg-card p-6 text-center">
            <h1 className="text-2xl font-bold">Hello World</h1>
            <p className="text-muted-foreground">Welcome to Yass - A multi-client card game.</p>
          </div>
        </div>
      </div>
    </>
  );
}
