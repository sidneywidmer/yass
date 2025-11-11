import { useNavigate } from "react-router-dom";

export function Logo() {
  const navigate = useNavigate();

  return (
    <button
      onClick={() => navigate("/")}
      className="self-center hover:opacity-80 transition-opacity"
    >
      <img src="/assets/logo.png" alt="Yass" className="h-24 w-auto" />
    </button>
  );
}
