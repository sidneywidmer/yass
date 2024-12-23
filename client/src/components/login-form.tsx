import {cn} from "@/lib/utils"
import {Button} from "@/components/ui/button"
import {Card, CardContent, CardHeader, CardTitle,} from "@/components/ui/card"
import {Input} from "@/components/ui/input"
import {Label} from "@/components/ui/label"
import {useTranslation} from "react-i18next";
import {useOry} from "@/hooks/use-ory.tsx";
import {AlertCircle} from "lucide-react";
import {Alert, AlertDescription, AlertTitle} from "@/components/ui/alert.tsx";
import {Link, useNavigate} from "react-router-dom";

export function LoginForm() {
  const {t} = useTranslation();
  const {login, loginError} = useOry()
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const formData = new FormData(e.target as HTMLFormElement)
    await login({
      email: formData.get('email') as string,
      password: formData.get('password') as string
    })
  }
  return (
    <>
      <div className="flex flex-col gap-6">
        <Card>
          <CardHeader className="text-center">
            <CardTitle className="text-xl">{t("auth.login.title")}</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit}>
              <div className="grid gap-6">
                <div className="flex flex-col gap-4">
                  <Button variant="outline" className="w-full"
                          onClick={() => navigate('/signup', {state: {isGuest: true}})}>
                    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"
                         fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round"
                         strokeLinejoin="round" className="lucide lucide-user">
                      <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/>
                      <circle cx="12" cy="7" r="4"/>
                    </svg>
                    {t("auth.login.guestPlay")}
                  </Button>
                </div>
                <div
                  className="relative text-center text-sm after:absolute after:inset-0 after:top-1/2 after:z-0 after:flex after:items-center after:border-t after:border-border">
                                <span className="relative z-10 bg-background px-2 text-muted-foreground">
                                    {t("auth.continueWith")}
                                </span>
                </div>
                {loginError && !loginError.field && (
                  <Alert variant="destructive">
                    <AlertCircle className="h-4 w-4"/>
                    <AlertTitle>{t("errors.title")}</AlertTitle>
                    <AlertDescription>
                      {loginError.text}
                    </AlertDescription>
                  </Alert>
                )}
                <div className="grid gap-6">
                  <div className="grid gap-2">
                    <Label htmlFor="email">{t("auth.form.email")}</Label>
                    <Input
                      name="email"
                      id="email"
                      type="email"
                      placeholder="trumpf@yass.app"
                      className={cn(loginError && loginError.field == "identifier" && "ring-2 ring-destructive")}
                      required
                    />
                  </div>
                  <div className="grid gap-2">
                    <div className="flex items-center">
                      <Label htmlFor="password">{t("auth.form.password")}</Label>
                      <a
                        href="#"
                        className="ml-auto text-sm underline-offset-4 hover:underline"
                      >
                        {t("auth.form.forgotPassword")}
                      </a>
                    </div>
                    <Input id="password" type="password" name="password"
                           className={cn(loginError && loginError.field == "password" && "ring-2 ring-destructive")}
                           required/>
                  </div>
                  <Button type="submit" className="w-full">
                    {t("auth.form.loginButton")}
                  </Button>
                </div>
                <div className="text-center text-sm">
                  {t("auth.signup.prompt")}&nbsp;
                  <Link to="/signup" className="underline underline-offset-4">
                    {t("auth.signup.link")}
                  </Link>
                </div>
              </div>
            </form>
          </CardContent>
        </Card>
      </div>
    </>
  )
}
