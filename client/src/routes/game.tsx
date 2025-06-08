import {GameInstance} from "@/components/game/game-instance.tsx";
import {useParams} from "react-router";

export default function Game() {
  const {code} = useParams()

  return (
    <>
      <GameInstance tryCode={code!!}/>
    </>
  );
}
