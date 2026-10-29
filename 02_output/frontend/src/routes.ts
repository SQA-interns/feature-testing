/** Whether the path is the organizer page (BR-09); every other path is the registration page. */
export function isOrganizerPath(pathname: string): boolean {
  return pathname === "/organizer" || pathname === "/organizer/";
}
