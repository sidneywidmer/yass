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

export const preloadAssets = () => {
  const suits = ['CLUBS', 'DIAMONDS', 'HEARTS', 'SPADES'];
  const ranks = ['ACE', 'TWO', 'THREE', 'FOUR', 'FIVE', 'SIX', 'SEVEN', 'EIGHT', 'NINE', 'TEN', 'JACK', 'QUEEN', 'KING'];
  const jokers = ['JOKER-2', 'JOKER-3'];
  const path = '/cards/french/';

  const promises: any[] = [];

  // Build the list of URLs first
  const imageUrls = [
    ...suits.flatMap(suit => ranks.map(rank => `${path}${suit}-${rank}.svg`)),
    ...jokers.map(joker => `${path}${joker}.svg`)
  ];

  console.log(`Preloading ${imageUrls.length} images...`);

  imageUrls.forEach(url => {
    const promise = new Promise((resolve, reject) => {
      const img = new Image();
      img.onload = () => resolve(img);
      img.onerror = () => reject(new Error(`Failed to load image: ${url}`));
      img.src = url;
    });
    promises.push(promise);
  });

  return Promise.all(promises);
}

