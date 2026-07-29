import {Card, CardContent, CardDescription, CardHeader, CardTitle} from "@/components/ui/card"
import {Button} from "@/components/ui/button"
import {Input} from "@/components/ui/input"
import {LogIn} from "lucide-react"
import {useTranslation} from "react-i18next"
import {useState} from "react"
import {useNavigate} from "react-router-dom"

export function JoinSection() {
  const {t} = useTranslation()
  const navigate = useNavigate()
  const [gameCode, setGameCode] = useState("")

  const handleJoinGame = (e: React.FormEvent) => {
    e.preventDefault()
    navigate(`/game/${gameCode}`)
  }

  return (
    <Card className="h-full">
      <CardHeader>
        <div className="flex items-center gap-2">
          <LogIn className="h-5 w-5 text-primary"/>
          <CardTitle className="text-lg">{t("lobby.join.title")}</CardTitle>
        </div>
        <CardDescription>{t("lobby.join.description")}</CardDescription>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleJoinGame} className="grid grid-cols-3 gap-3">
          <Input
            value={gameCode}
            onChange={(e) => setGameCode(e.target.value.toUpperCase())}
            maxLength={5}
            placeholder={t("main.game.codePlaceholder")}
            className="col-span-2 text-center text-lg font-medium tracking-[0.25em] h-10"
          />
          <Button type="submit" disabled={gameCode.length !== 5}>
            {t("main.game.join")}
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}
