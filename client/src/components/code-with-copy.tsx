import {Check, Copy} from 'lucide-react';
import {Button} from "@/components/ui/button.tsx";
import {useState} from "react";

interface CodeWithCopyProps {
  code: string;
  copyFullUrl?: boolean;
}

const CodeWithCopy = ({code, copyFullUrl = false}: CodeWithCopyProps) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    const textToCopy = copyFullUrl
      ? `${window.location.origin}/game/${code}`
      : code;
    await navigator.clipboard.writeText(textToCopy);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className={`inline-flex items-center gap-2`}>
      <code className="font-mono text-sm bg-muted px-2 py-1 rounded"> {code} </code>
      <Button
        variant="ghost"
        size="icon"
        className="h-6 w-6"
        onClick={handleCopy}
      >
        {copied ? (
          <Check className="h-3 w-3 text-green-500"/>
        ) : (
          <Copy className="h-3 w-3"/>
        )}
      </Button>
    </div>
  );
};

export default CodeWithCopy;
