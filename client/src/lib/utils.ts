import {clsx, type ClassValue} from "clsx"
import {twMerge} from "tailwind-merge"
import {Position} from "@/api/generated";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

const POSITIONS: Position[] = ['NORTH', 'EAST', 'SOUTH', 'WEST']
export const getRelativePosition = (playerPosition: Position, absolutePosition: Position): Position => {
  let southOffset = 2 - POSITIONS.indexOf(playerPosition!!)
  let newPos = POSITIONS.indexOf(absolutePosition) + southOffset

  if (newPos > 3) {
    newPos = newPos % 4
  } else if (newPos == -1) {
    newPos = 3
  }

  return POSITIONS[newPos]
}


export const getResponsiveValue = (minValue: number, maxValue: number) => {
  const currentWidth = window.innerWidth;
  const min = 375
  const max = 600
  if (currentWidth <= min) {
    return minValue;
  }
  if (currentWidth >= max) {
    return maxValue;
  }

  const slope = (maxValue - minValue) / (max - min);
  return minValue + slope * (currentWidth - min);
}

