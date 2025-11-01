import {GalleryVerticalEnd} from "lucide-react";

export default function Landing() {
  return (
    <>
      <div className="flex min-h-svh flex-col items-center justify-center gap-6 bg-muted p-6 md:p-10">
        <div className="flex w-full max-w-sm flex-col gap-6">
          <a href="#" className="flex items-center gap-2 self-center font-medium">
            <div
              className="flex h-6 w-6 items-center justify-center rounded-md bg-primary text-primary-foreground">
              <GalleryVerticalEnd className="size-4"/>
            </div>
            Yass
          </a>
          <div className="flex flex-col gap-4 rounded-lg border bg-card p-6 text-center">
            <h1 className="text-2xl font-bold">Hello World</h1>
            <p className="text-muted-foreground">Welcome to Yass - A multi-client card game.</p>
          </div>
        </div>
      </div>
    </>
  );
}
