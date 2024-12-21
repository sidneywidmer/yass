export default function Root() {
  return (
    <>
      <div className="min-h-screen bg-background">
        <header className="border-b">
          <div className="container flex h-16 items-center px-4">
            <div className="flex-1">
              <h1 className="text-lg font-bold">Your App</h1>
            </div>
          </div>
        </header>
        <main className="container py-6 px-4">
          <p>Hello</p>
        </main>
      </div>
    </>
  );
}
