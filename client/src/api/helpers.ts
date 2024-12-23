import {UiNode, UiNodeInputAttributes, UiText} from "@ory/client";
import {TFunction} from "i18next";

export type ErrorMessage = {
  id: number;
  text: string;
  context?: Record<string, any>;
  field?: string;
}

type ErrorDetails = {
  ui: UiText;
  field?: string;
}

const findErrorMessage = (nodes: UiNode[] = []): ErrorDetails | null => {
  const nodeWithError = nodes.find(n => n.messages?.some(m => m.type === 'error'));

  if (!nodeWithError) {
    return null
  }

  return {
    ui: nodeWithError.messages.find(m => m.type === 'error')!,
    field: (nodeWithError.attributes as UiNodeInputAttributes).name
  };
};

export const getOryErrorMessage = (data: any, t: TFunction): ErrorMessage | null => {
  const topError: UiText = data?.ui?.messages?.find((msg: UiText) => msg.type === 'error');
  const error = topError ? {ui: topError} as ErrorDetails : findErrorMessage(data?.ui?.nodes);

  return error ? {
    id: error.ui.id,
    text: t(`errors.ory.${error.ui.id}`, {...error.ui.context ?? {}}),
    context: error.ui.context,
    field: error.field
  } : null;
};

export const generateAnonToken = () => {
  const tokenBytes = new Uint8Array(128)
  crypto.getRandomValues(tokenBytes)
  return `anonToken_${btoa(String.fromCharCode(...tokenBytes))}`
}

