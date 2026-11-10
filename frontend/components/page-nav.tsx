"use client";

import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination";

interface PageNavProps {
  page: number; // 1-based
  totalPages: number;
  onChange: (page: number) => void;
}

function pageNumbers(page: number, totalPages: number): (number | "ellipsis")[] {
  if (totalPages <= 5) return Array.from({ length: totalPages }, (_, i) => i + 1);

  const numbers: (number | "ellipsis")[] = [1];
  if (page > 3) numbers.push("ellipsis");
  for (let i = Math.max(2, page - 1); i <= Math.min(totalPages - 1, page + 1); i++) numbers.push(i);
  if (page < totalPages - 2) numbers.push("ellipsis");
  numbers.push(totalPages);
  return numbers;
}

export default function PageNav({ page, totalPages, onChange }: PageNavProps) {
  if (totalPages <= 1) return null;

  const go = (target: number) => (e: React.MouseEvent) => {
    e.preventDefault();
    onChange(target);
  };

  return (
    <div className="mt-8">
      <Pagination>
        <PaginationContent>
          {page > 1 && (
            <PaginationItem>
              <PaginationPrevious href="#" onClick={go(page - 1)} />
            </PaginationItem>
          )}
          {pageNumbers(page, totalPages).map((n, index) => (
            <PaginationItem key={`${n}-${index}`}>
              {n === "ellipsis" ? (
                <PaginationEllipsis />
              ) : (
                <PaginationLink href="#" isActive={n === page} onClick={go(n)}>
                  {n}
                </PaginationLink>
              )}
            </PaginationItem>
          ))}
          {page < totalPages && (
            <PaginationItem>
              <PaginationNext href="#" onClick={go(page + 1)} />
            </PaginationItem>
          )}
        </PaginationContent>
      </Pagination>
    </div>
  );
}
