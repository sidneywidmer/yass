import {UiNode, UiNodeInputAttributes, UiText} from "@ory/client";
import {TFunction} from "i18next";

export type ErrorMessage = {
  id: number;
  text: string;
  context?: Record<string, unknown>;
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

export const getOryErrorMessage = (data: { ui?: { messages?: UiText[]; nodes?: UiNode[] } } | null | undefined, t: TFunction): ErrorMessage | null => {
  const topError = data?.ui?.messages?.find((msg: UiText) => msg.type === 'error');
  const error = topError ? {ui: topError} as ErrorDetails : findErrorMessage(data?.ui?.nodes);

  return error ? {
    id: error.ui.id,
    text: t(`errors.ory.${error.ui.id}`, {...(error.ui.context as Record<string, unknown> ?? {})}),
    context: error.ui.context as Record<string, unknown> | undefined,
    field: error.field
  } : null;
};