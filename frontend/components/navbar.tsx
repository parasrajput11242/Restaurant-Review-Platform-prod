"use client";

import Link from "next/link";
import { useAuth } from "react-oidc-context";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Badge } from "@/components/ui/badge";
import { useRoles } from "@/hooks/use-roles";

export default function Navbar() {
  const auth = useAuth();
  const { isAuthenticated, isLoading, isAdmin, isOwner, username } = useRoles();

  const roleLabel = isAdmin ? "Admin" : isOwner ? "Restaurant owner" : "Member";

  return (
    <header className="border-b">
      <div className="max-w-[1200px] mx-auto px-4 py-4 flex justify-between items-center">
        <Link href="/" className="text-2xl font-bold">
          RestaurantReviews
        </Link>

        {isLoading ? null : isAuthenticated ? (
          <DropdownMenu>
            <DropdownMenuTrigger aria-label="Account menu" className="rounded-full">
              <Avatar>
                <AvatarFallback>{(username ?? "U").slice(0, 1).toUpperCase()}</AvatarFallback>
              </Avatar>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="min-w-[200px]">
              <DropdownMenuLabel className="flex flex-col gap-1">
                <span>{username}</span>
                <Badge variant="secondary" className="w-fit">
                  {roleLabel}
                </Badge>
              </DropdownMenuLabel>
              <DropdownMenuSeparator />
              {isOwner && (
                <>
                  <DropdownMenuItem asChild>
                    <Link href="/my-restaurants">My restaurants</Link>
                  </DropdownMenuItem>
                  <DropdownMenuItem asChild>
                    <Link href="/restaurants/create">Add restaurant</Link>
                  </DropdownMenuItem>
                </>
              )}
              {!isOwner && (
                <DropdownMenuItem asChild>
                  <Link href="/become-owner">Become a restaurant owner</Link>
                </DropdownMenuItem>
              )}
              {isAdmin && (
                <DropdownMenuItem asChild>
                  <Link href="/admin">Owner requests</Link>
                </DropdownMenuItem>
              )}
              <DropdownMenuSeparator />
              <DropdownMenuItem onClick={() => void auth.signoutRedirect()}>Log out</DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        ) : (
          <div className="flex gap-2">
            <Button variant="outline" onClick={() => void auth.signinRedirect({ prompt: "create" })}>
              Sign up
            </Button>
            <Button onClick={() => void auth.signinRedirect()}>Log in</Button>
          </div>
        )}
      </div>
    </header>
  );
}
