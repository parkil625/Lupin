/**
 * WorkoutTypeSelect - 운동 종류 선택 컴포넌트
 * Molecule: Label + Popover + Command
 */

import { useState } from "react";
import { Label } from "@/components/ui/label";
import { Button } from "@/components/ui/button";
import { Check, ChevronsUpDown } from "lucide-react";
import { cn } from "@/lib/utils";
import {
  Command,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
} from "@/components/ui/command";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";

export const WORKOUT_TYPES = [
  { value: "running", label: "런닝" },
  { value: "walking", label: "걷기" },
  { value: "cycling", label: "사이클" },
  { value: "swimming", label: "수영" },
  { value: "weight", label: "웨이트" },
  { value: "yoga", label: "요가" },
  { value: "pilates", label: "필라테스" },
  { value: "crossfit", label: "크로스핏" },
  { value: "hiking", label: "등산" },
  { value: "other", label: "기타" },
];

interface WorkoutTypeSelectProps {
  value: string;
  onChange: (value: string) => void;
  label?: string;
  className?: string;
}

export default function WorkoutTypeSelect({
  value,
  onChange,
  label = "운동 종류",
  className = "",
}: WorkoutTypeSelectProps) {
  const [open, setOpen] = useState(false);

  const selectedLabel = WORKOUT_TYPES.find((type) => type.value === value)?.label || "운동 선택";

  return (
    <div className={className}>
      <Label className="text-xs font-bold text-gray-900 mb-2 block">{label}</Label>
      <Popover open={open} onOpenChange={setOpen}>
        <PopoverTrigger asChild>
          <Button
            variant="outline"
            role="combobox"
            aria-expanded={open}
            className="w-full justify-between bg-white border-gray-300 text-sm"
          >
            {selectedLabel}
            <ChevronsUpDown className="ml-2 h-4 w-4 shrink-0 opacity-50" />
          </Button>
        </PopoverTrigger>
        <PopoverContent className="w-full p-0" style={{ width: "var(--radix-popover-trigger-width)" }}>
          <Command>
            <CommandInput placeholder="운동 검색..." />
            <CommandList>
              <CommandEmpty>운동을 찾을 수 없습니다.</CommandEmpty>
              <CommandGroup>
                {WORKOUT_TYPES.map((type) => (
                  <CommandItem
                    key={type.value}
                    value={type.value}
                    onSelect={(currentValue: string) => {
                      onChange(currentValue === value ? "" : currentValue);
                      setOpen(false);
                    }}
                  >
                    <Check
                      className={cn(
                        "mr-2 h-4 w-4",
                        value === type.value ? "opacity-100" : "opacity-0"
                      )}
                    />
                    {type.label}
                  </CommandItem>
                ))}
              </CommandGroup>
            </CommandList>
          </Command>
        </PopoverContent>
      </Popover>
    </div>
  );
}
