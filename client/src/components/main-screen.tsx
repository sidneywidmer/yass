import {Button} from "@/components/ui/button"
import {Card, CardContent, CardHeader, CardTitle,} from "@/components/ui/card"
import {Input} from "@/components/ui/input"
import {useState} from "react";
import {useTranslation} from "react-i18next";
import {usePlayerStore} from "@/store/player.ts";
import {CreateGameOverlay} from "@/components/create-game-overlay.tsx";
import {useNavigate} from "react-router-dom";

export function MainScreen() {
  const [gameCode, setGameCode] = useState("")
  const name = usePlayerStore(state => state.name)
  const {t} = useTranslation()
  const navigate = useNavigate()

  const handleJoinGame = (e: React.FormEvent) => {
    e.preventDefault()
    navigate(`/game/${gameCode}`)
  }

  return (
    <>
      <div className="container max-w-lg mx-auto p-4">
        <Card>
          <CardHeader className="text-center">
            <CardTitle className="text-xl">{t("main.title")}, {name}</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex flex-col gap-4">
              <form onSubmit={handleJoinGame} className="flex gap-2">
                <Input
                  value={gameCode}
                  onChange={(e) => setGameCode(e.target.value.toUpperCase())}
                  maxLength={5}
                  placeholder={t("main.game.codePlaceholder")}
                  className="flex-1 text-center text-lg tracking-wider"
                />
                <Button type="submit" disabled={gameCode.length !== 5}>
                  {t("main.game.join")}
                </Button>
              </form>
              <CreateGameOverlay/>
            </div>
          </CardContent>
        </Card>
      </div>
    </>
  )
}
