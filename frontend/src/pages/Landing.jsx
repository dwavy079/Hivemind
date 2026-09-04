import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import Logo from '../components/Logo'

const FEATURES = [
  {
    title: 'Upload & organize',
    desc: 'Drag files in, sort them into folders, and find anything again in seconds.',
    icon: (
      <path
        d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V7Z"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinejoin="round"
      />
    ),
  },
  {
    title: 'Never lose a version',
    desc: 'Re-upload a file and the old copy stays put — every version is one click away.',
    icon: (
      <>
        <path d="M12 7v5l3.5 2" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" />
        <circle cx="12" cy="12" r="9" stroke="currentColor" strokeWidth="1.7" />
      </>
    ),
  },
  {
    title: 'Share with a link',
    desc: "Send a link to anyone — no account needed on their end. Set it to expire, or revoke it any time.",
    icon: (
      <path
        d="M9 12a3 3 0 0 0 4.24.2l3-3a3 3 0 0 0-4.24-4.24L10.5 6.5M15 12a3 3 0 0 0-4.24-.2l-3 3a3 3 0 0 0 4.24 4.24L13.5 17.5"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    ),
  },
  {
    title: 'Backed by real cloud storage',
    desc: 'Every file lives in Amazon S3 behind your own account — not a database blob.',
    icon: (
      <path
        d="M12 3 4 6.5v6C4 16.7 7.4 20 12 21c4.6-1 8-4.3 8-8.5v-6L12 3Z"
        stroke="currentColor"
        strokeWidth="1.7"
        strokeLinejoin="round"
      />
    ),
  },
]

export default function Landing() {
  const { session } = useAuth()

  return (
    <div className="landing">
      <nav className="landing-nav">
        <div className="brand">
          <span className="brand-mark">
            <Logo size={15} />
          </span>
          HiveMind
        </div>
        {session ? (
          <Link to="/app" className="btn btn-primary">
            Open HiveMind
          </Link>
        ) : (
          <div style={{ display: 'flex', gap: 10 }}>
            <Link to="/login" className="btn">
              Log in
            </Link>
            <Link to="/register" className="btn btn-primary">
              Sign up
            </Link>
          </div>
        )}
      </nav>

      <header className="landing-hero">
        <span className="landing-badge">Upload once. Organize forever. Share in a click.</span>
        <h1>
          One hive for
          <br />
          all your files.
        </h1>
        <p>
          HiveMind keeps every file, every version, and every folder in one place — then hands out a
          link the moment someone else needs it.
        </p>
        <div className="landing-cta">
          <Link to={session ? '/app' : '/register'} className="btn btn-primary btn-lg">
            {session ? 'Open HiveMind' : 'Get started — it’s free'}
          </Link>
          <a href="#features" className="btn btn-lg">
            See how it works
          </a>
        </div>
      </header>

      <section className="landing-features" id="features">
        {FEATURES.map((f) => (
          <div className="feature-card" key={f.title}>
            <div className="feature-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                {f.icon}
              </svg>
            </div>
            <h3>{f.title}</h3>
            <p>{f.desc}</p>
          </div>
        ))}
      </section>

      <footer className="landing-footer">
        <span>HiveMind — built with Spring Boot, React &amp; AWS S3</span>
      </footer>
    </div>
  )
}
