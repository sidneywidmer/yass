import {useEffect, useRef} from "react";
import {useTranslation} from "react-i18next";
import {Navigate, useNavigate, useParams} from "react-router-dom";
import {motion} from "motion/react";
import {CalendarDays, LogIn, Plus} from "lucide-react";
import {cn} from "@/lib/utils";
import {DailyTab} from "@/components/lobby/daily-tab.tsx";
import {JoinSection} from "@/components/lobby/join-section.tsx";
import {CreateSection} from "@/components/lobby/create-section.tsx";

const sections = [
  {id: "daily" as const, icon: CalendarDays, Section: DailyTab},
  {id: "join" as const, icon: LogIn, Section: JoinSection},
  {id: "create" as const, icon: Plus, Section: CreateSection},
]

export function MainScreen() {
  const {t} = useTranslation()
  const {section} = useParams()
  const navigate = useNavigate()
  const trackRef = useRef<HTMLDivElement>(null)
  const alignedRef = useRef(false)
  const fromScrollRef = useRef(false)
  const targetRef = useRef<number | null>(null)

  const active = sections.find(s => s.id === section)?.id

  // Aligns the carousel whenever the section changes through the URL, be it a tab click, a browser
  // back or a deep link. The first alignment jumps so a deep link does not animate on load.
  useEffect(() => {
    const track = trackRef.current
    if (!track || !active) return

    // A section change that came from swiping is already on screen. Scrolling the track again would
    // fight the momentum of the ongoing gesture and carry it past the section the swipe aimed at.
    if (fromScrollRef.current) {
      fromScrollRef.current = false
      alignedRef.current = true
      return
    }

    const left = sections.findIndex(s => s.id === active) * track.clientWidth
    targetRef.current = left
    track.scrollTo({left, behavior: alignedRef.current ? "smooth" : "auto"})
    alignedRef.current = true

    // The target is normally cleared once the track arrives. This releases it in case the animation
    // never reports the exact position, so a swipe afterwards is not ignored forever.
    const timeout = setTimeout(() => (targetRef.current = null), 700)
    return () => clearTimeout(timeout)
  }, [active])

  // Keeps the navigation in sync while swiping through the carousel on mobile. On desktop only the
  // active section is rendered, so the track never overflows and this is a no-op.
  const handleScroll = () => {
    const track = trackRef.current
    if (!track || track.scrollWidth <= track.clientWidth) return

    // While the track animates towards a tapped section it travels over the ones in between.
    // Syncing the navigation to those would drag the indicator back and forth before it settles.
    if (targetRef.current !== null) {
      if (Math.abs(track.scrollLeft - targetRef.current) <= 2) targetRef.current = null
      return
    }

    const id = sections[Math.round(track.scrollLeft / track.clientWidth)]?.id
    if (!id || id === active) return

    fromScrollRef.current = true
    navigate(`/lobby/${id}`, {replace: true})
  }

  if (!active) return <Navigate to="/lobby/daily" replace/>

  return (
    <div className="flex w-full flex-col gap-4">
      <nav role="tablist" className="flex items-center gap-1 rounded-lg border bg-card p-1 shadow-sm">
        {sections.map(({id, icon: Icon}) => (
          <button
            key={id}
            type="button"
            role="tab"
            aria-selected={active === id}
            onClick={() => navigate(`/lobby/${id}`)}
            className={cn(
              "relative flex-1 cursor-pointer select-none touch-manipulation rounded-md px-2 py-2",
              "text-xs font-medium transition-colors sm:px-3 sm:text-sm",
              "focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring",
              active === id ? "text-primary-foreground" : "text-muted-foreground hover:text-foreground"
            )}
          >
            {active === id && (
              <motion.span
                layoutId="lobby-tab-indicator"
                className="pointer-events-none absolute inset-0 rounded-md bg-primary shadow"
                transition={{type: "spring", stiffness: 400, damping: 35}}
              />
            )}
            <span className="relative flex items-center justify-center gap-2">
              <Icon className="hidden h-4 w-4 sm:block"/>
              {t(`lobby.tabs.${id}`)}
            </span>
          </button>
        ))}
      </nav>

      <div
        ref={trackRef}
        onScroll={handleScroll}
        className="flex items-start overflow-x-auto overflow-y-hidden snap-x snap-mandatory [scrollbar-width:none] md:overflow-visible [&::-webkit-scrollbar]:hidden"
      >
        {sections.map(({id, Section}) => (
          <div
            key={id}
            className={cn("w-full shrink-0 snap-center", active !== id && "md:hidden")}
          >
            <Section/>
          </div>
        ))}
      </div>
    </div>
  )
}
