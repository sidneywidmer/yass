import {Globe} from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {useLangStore} from "@/store/language.ts";
import {SupportedLanguage} from "@/types/language.ts";

const LanguageSwitcher = () => {
  const { setLanguage } = useLangStore();

  return (
    <div className="fixed top-4 right-4">
      <DropdownMenu>
        <DropdownMenuTrigger
          className="flex items-center gap-2 px-3 py-2 text-sm font-medium rounded-md border shadow-sm hover:bg-gray-50">
          <Globe className="w-4 h-4"/>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem className="flex items-center gap-2" onSelect={() => setLanguage(SupportedLanguage.EN)}>
            <img
              src="https://flagcdn.com/w20/gb.png"
              width="16"
              height="12"
              alt="English"
              className="inline-block"
            />
            English
          </DropdownMenuItem>
          <DropdownMenuItem className="flex items-center gap-2" onSelect={() => setLanguage(SupportedLanguage.DE)}>
            <img
              src="https://flagcdn.com/w20/de.png"
              width="16"
              height="12"
              alt="Deutsch"
              className="inline-block"
            />
            Deutsch
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>
  );
};

export default LanguageSwitcher;