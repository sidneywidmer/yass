import {State} from "@/api/generated";

// Typed mirror of the generated State union so components don't compare against bare strings.
// `satisfies` breaks the build if the generated union ever changes.
export const GameStates = {
  WAITING_FOR_PLAYERS: 'WAITING_FOR_PLAYERS',
  PLAY_CARD: 'PLAY_CARD',
  PLAY_CARD_BOT: 'PLAY_CARD_BOT',
  SCHIEBE: 'SCHIEBE',
  SCHIEBE_BOT: 'SCHIEBE_BOT',
  WEISEN_FIRST: 'WEISEN_FIRST',
  WEISEN_FIRST_BOT: 'WEISEN_FIRST_BOT',
  WEISEN_SECOND: 'WEISEN_SECOND',
  WEISEN_SECOND_BOT: 'WEISEN_SECOND_BOT',
  TRUMP: 'TRUMP',
  TRUMP_BOT: 'TRUMP_BOT',
  NEW_TRICK: 'NEW_TRICK',
  NEW_HAND: 'NEW_HAND',
  FINISHED: 'FINISHED',
} as const satisfies { [S in State]: S };
