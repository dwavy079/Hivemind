// The HiveMind mark: a hexagon (the "hive") with a small connected-node
// cluster inside (the "mind" — many points working as one).
export default function Logo({ size = 20 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path
        d="M16 2 28 9v14l-12 7-12-7V9L16 2Z"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinejoin="round"
      />
      <circle cx="16" cy="12" r="2.1" fill="currentColor" />
      <circle cx="10.5" cy="19" r="2.1" fill="currentColor" />
      <circle cx="21.5" cy="19" r="2.1" fill="currentColor" />
      <path
        d="M16 12 10.5 19M16 12l5.5 7M10.5 19h11"
        stroke="currentColor"
        strokeWidth="1.4"
        strokeLinecap="round"
      />
    </svg>
  )
}
