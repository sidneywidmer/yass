export enum PlaySpeed {
  SLOW = 'slow',
  MEDIUM = 'medium',
  FAST = 'fast'
}

export enum GameDelay {
  // Pause before clearing the played cards off the table
  CLEAR_TABLE = 'clearTable',
  // Gap between processing queued game actions
  NEXT_ACTION = 'nextAction',
  // Hold the winning card's shake animation on screen before it slides off the table
  WINNER_HOLD = 'winnerHold'
}

export const playSpeedTimings: Record<PlaySpeed, Record<GameDelay, number>> = {
  [PlaySpeed.SLOW]: {[GameDelay.CLEAR_TABLE]: 2000, [GameDelay.NEXT_ACTION]: 100, [GameDelay.WINNER_HOLD]: 1200},
  [PlaySpeed.MEDIUM]: {[GameDelay.CLEAR_TABLE]: 1000, [GameDelay.NEXT_ACTION]: 50, [GameDelay.WINNER_HOLD]: 800},
  [PlaySpeed.FAST]: {[GameDelay.CLEAR_TABLE]: 300, [GameDelay.NEXT_ACTION]: 25, [GameDelay.WINNER_HOLD]: 400}
}
