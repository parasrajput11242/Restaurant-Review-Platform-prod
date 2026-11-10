import { Star } from "lucide-react";

export default function Stars({ value = 0, size = 4 }: { value?: number; size?: 4 | 5 }) {
  const dimension = size === 5 ? "w-5 h-5" : "w-4 h-4";
  return (
    <div className="flex" aria-label={`${value} out of 5 stars`}>
      {Array.from({ length: 5 }).map((_, i) => (
        <Star
          key={i}
          className={`${dimension} ${i < Math.round(value) ? "text-yellow-400 fill-yellow-400" : "text-gray-300"}`}
        />
      ))}
    </div>
  );
}
