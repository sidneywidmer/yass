import {useEffect, useState} from 'react'
import {Button} from "@/components/ui/button"
import {Card, CardContent, CardHeader, CardTitle} from "@/components/ui/card"
import {Input} from "@/components/ui/input"
import {Label} from "@/components/ui/label"
import {Switch} from "@/components/ui/switch"
import {useOry} from '@/hooks/use-ory.tsx'
import {useTranslation} from 'react-i18next'
import {useLocation, useNavigate} from "react-router";
import {Tooltip, TooltipContent, TooltipProvider, TooltipTrigger} from "@/components/ui/tooltip.tsx";
import {AlertCircle, HelpCircle, Loader2} from "lucide-react";
import {Link} from "react-router-dom";
import {cn, getValidRedirectPath} from "@/lib/utils.ts";
import {Alert, AlertDescription, AlertTitle} from "@/components/ui/alert.tsx";
import {useAnon} from "@/hooks/use-anon.tsx";
import {useAsyncAction} from "@/hooks/use-async-action";
import {useAuth} from "@/hooks/use-auth.tsx";
import {usePlayerStore} from "@/store/player.ts";

export function SignupForm() {
  const location = useLocation()
  const navigate = useNavigate()
  const [isAnonSignup, setIsAnonSignup] = useState(location.state?.isAnonSignup || false)
  const {t} = useTranslation()
  const {signup, upgradeAnon, signupError} = useOry()
  const {anonSignup, anonSignupError} = useAnon()
  const {isAuthenticated, initialized} = useAuth()
  const isAnon = usePlayerStore(state => state.isAnon)
  const anonName = usePlayerStore(state => state.name)

  // A logged in anon player lands here to upgrade the existing profile instead of creating a new one.
  const isUpgrade = isAuthenticated && isAnon

  const redirectTo = getValidRedirectPath(location.state?.from)

  useEffect(() => {
    if (initialized && isAuthenticated && !isAnon) {
      navigate('/lobby', {replace: true})
    }
  }, [initialized, isAuthenticated, isAnon, navigate])

  const {execute: executeSignup, isLoading, hasError, reset} = useAsyncAction(async (data: {username: string, email?: string, password?: string}) => {
    if (isUpgrade) {
      return upgradeAnon({
        username: data.username,
        email: data.email!,
        password: data.password!
      }, redirectTo || '/lobby')
    }
    if (isAnonSignup) {
      return anonSignup(data.username, redirectTo)
    }
    return signup({
      username: data.username,
      email: data.email!,
      password: data.password!
    }, redirectTo)
  })

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const formData = new FormData(e.target as HTMLFormElement)
    reset()
    await executeSignup({
      username: formData.get('username') as string,
      email: formData.get('email') as string,
      password: formData.get('password') as string
    })
  }

  if (!initialized) {
    return (
      <div className="flex min-h-[400px] items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin" />
      </div>
    )
  }

  return (
    <Card>
      <CardHeader className="text-center">
        <CardTitle className="text-xl">{t(isUpgrade ? 'auth.upgrade.title' : 'auth.signup.title')}</CardTitle>
        {isUpgrade && (
          <p className="text-sm text-muted-foreground">{t('auth.upgrade.description')}</p>
        )}
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit}>
          <div className="grid gap-6">

            {signupError && !signupError.field && (
              <Alert variant="destructive">
                <AlertCircle className="h-4 w-4"/>
                <AlertTitle>{t("errors.title")}</AlertTitle>
                <AlertDescription>
                  {signupError?.text}
                </AlertDescription>
              </Alert>
            )}
            <div className="grid gap-2">
              <Label htmlFor="username">{t('auth.form.username')}</Label>
              <Input
                className={cn((signupError?.field == "traits.name" || !!anonSignupError) && "ring-2 ring-destructive")}
                name="username"
                id="username"
                type="text"
                defaultValue={isUpgrade ? anonName : undefined}
                required
              />
              {isUpgrade && (
                <p className="text-xs text-muted-foreground">{t('auth.upgrade.usernameHint')}</p>
              )}
              {(signupError?.field == "traits.name" || anonSignupError) && (
                <p className="text-sm font-medium text-destructive">
                  {signupError?.text || anonSignupError?.text}
                </p>
              )}
            </div>

            {!isUpgrade && (
              <div className="flex items-center space-x-2">
                <Switch
                  id="anon-mode"
                  checked={isAnonSignup}
                  onCheckedChange={setIsAnonSignup}
                />
                <Label htmlFor="anon-mode">{t('auth.signup.anonProfile')}</Label>
                <TooltipProvider>
                  <Tooltip>
                    <TooltipTrigger>
                      <HelpCircle className="h-4 w-4 text-muted-foreground"/>
                    </TooltipTrigger>
                    <TooltipContent>
                      <p className="whitespace-pre-line">{t('auth.signup.anonProfileInfo')}</p>
                    </TooltipContent>
                  </Tooltip>
                </TooltipProvider>
              </div>
            )}
            {(!isAnonSignup || isUpgrade) && (
              <>
                <div className="grid gap-2">
                  <Label htmlFor="email">{t('auth.form.email')}</Label>
                  <Input
                    className={cn(signupError?.field == "traits.email" && "ring-2 ring-destructive")}
                    name="email"
                    id="email"
                    type="email"
                    required
                  />
                  {signupError?.field == "traits.email" && (
                    <p className="text-sm font-medium text-destructive">
                      {signupError.text}
                    </p>
                  )}
                </div>

                <div className="grid gap-2">
                  <Label htmlFor="password">{t('auth.form.password')}</Label>
                  <Input
                    className={cn(signupError?.field == "password" && "ring-2 ring-destructive")}
                    id="password"
                    name="password"
                    type="password"
                    required
                  />
                  {signupError?.field == "password" && (
                    <p className="text-sm font-medium text-destructive">
                      {signupError.text}
                    </p>
                  )}
                </div>
              </>
            )}

            <Button 
              type="submit" 
              className="w-full"
              disabled={isLoading}
              variant={hasError ? "destructive" : "default"}
            >
              {isLoading && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              {t(isUpgrade ? 'auth.upgrade.submit' : 'auth.signup.submit')}
            </Button>

            {isUpgrade ? (
              <Button type="button" variant="ghost" onClick={() => navigate('/lobby')}>
                {t("auth.upgrade.cancel")}
              </Button>
            ) : (
              <div className="text-center text-sm">
                {t("auth.login.prompt")}&nbsp;
                <Link to="/login" state={{from: location.state?.from}} className="underline underline-offset-4">
                  {t("auth.login.link")}
                </Link>
              </div>
            )}
          </div>
        </form>
      </CardContent>
    </Card>
  )
}
