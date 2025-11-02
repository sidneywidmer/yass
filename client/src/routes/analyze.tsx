import {useEffect, useState} from 'react'
import {api} from '@/api/client'
import {useAxiosErrorHandler} from "@/hooks/use-axios-error-handler.tsx";
import {useParams} from "react-router";
import {useNavigate} from "react-router-dom";
import {AnalyzeInstance} from "@/components/analyze/analyze-instance";
import {AnalyzeGameStateResponse} from "@/api/generated";

export default function Analyze() {
  const [analysis, setAnalysis] = useState<AnalyzeGameStateResponse | null>(null)
  const handleAxiosError = useAxiosErrorHandler()
  const {code} = useParams()
  const navigate = useNavigate()

  useEffect(() => {
    api.analyzeGame(code!!)
      .then(response => setAnalysis(response.data!))
      .catch(error => {
        navigate("/lobby")
        handleAxiosError(error)
      })
  }, [code])

  if (!analysis) return <div>Loading...</div>

  return <AnalyzeInstance code={code!!} analysis={analysis} />
}
